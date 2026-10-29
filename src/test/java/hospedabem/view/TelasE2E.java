package hospedabem.view;

import hospedabem.BancoDeTeste;
import hospedabem.controller.Sessao;
import hospedabem.dao.InicializadorBanco;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de ponta a ponta: telas JavaFX reais → controladores → DAOs → PostgreSQL de teste.
 * Cada caso entra pela tela de login, como o funcionário, e confere o resultado na tela e no banco.
 */
@SpringBootTest
@ActiveProfiles("test")
class TelasE2E {
    static final LocalDate HOJE = LocalDate.now();

    @Autowired ConfigurableApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired InicializadorBanco inicializador;
    @Autowired Sessao sessao;
    RoboFx robo;

    @BeforeAll
    static void iniciarJavaFx() throws InterruptedException {
        RoboFx.iniciarJavaFx();
    }

    @BeforeEach
    void abrirNaTelaDeLogin() {
        BancoDeTeste.recriar(jdbc, inicializador);
        sessao.encerrar();
        robo = RoboFx.abrir(RoboFx.naFx(() -> new TelaLogin(contexto)));
    }

    @AfterEach
    void fechar() {
        robo.fechar();
    }

    void entrar(String matricula, String senha) {
        robo.escrever("Matrícula", matricula);
        robo.escrever("Senha", senha);
        robo.clicar("Entrar");
    }

    String consultar(String sql, Object... args) {
        return String.valueOf(jdbc.queryForObject(sql, Object.class, args));
    }

    @Test
    void loginRecusaSenhaErradaERecepcionistaNaoAcessaCadastros() {
        entrar("admin01", "errada");
        assertEquals("Matrícula ou senha inválida.", robo.aviso());
        robo.capturar("login-senha-incorreta");

        entrar("recep01", "Recep@123");
        assertTrue(robo.exibeTexto("Recepcionista"));
        assertTrue(robo.desabilitado("Tipos de quarto"));
        assertTrue(robo.desabilitado("Quartos"));
        robo.capturar("recepcionista-sem-cadastros");

        robo.clicar("Sair");
        assertTrue(robo.exibeTexto("Sistema de hospedagem"));
    }

    @Test
    void administradorCadastraTipoEQuartoEConsultaDisponibilidade() {
        entrar("admin01", "Admin@123");
        robo.clicar("Tipos de quarto");
        robo.escrever("Código", "FAM");
        robo.escrever("Descrição", "Família");
        robo.escrever("Capacidade máxima", "5");
        robo.escrever("Valor da diária (R$)", "450,00");
        robo.clicar("Salvar");
        assertEquals("Tipo FAM gravado.", robo.aviso());
        robo.capturar("tipos-de-quarto");

        robo.escrever("Código", "STD");
        robo.escrever("Descrição", "Repetido");
        robo.escrever("Capacidade máxima", "2");
        robo.escrever("Valor da diária (R$)", "100");
        robo.clicar("Salvar");
        assertEquals("Já existe tipo de quarto com o código STD.", robo.aviso());

        robo.clicar("Quartos");
        robo.escrever("Número", "401");
        robo.escrever("Andar", "4");
        robo.escolher("Tipo", t -> t.toString().startsWith("FAM"));
        robo.clicar("Salvar");
        assertEquals("Quarto 401 gravado.", robo.aviso());
        robo.escrever("Número", "402");
        robo.escrever("Andar", "4");
        robo.escolher("Tipo", t -> t.toString().startsWith("FAM"));
        robo.escolher("Situação", s -> s.toString().equals("OCUPADO"));
        robo.clicar("Salvar");
        assertTrue(robo.aviso().startsWith("Situação OCUPADO não pode ser definida pelo cadastro"));
        robo.capturar("quartos");

        robo.clicar("Disponibilidade");
        robo.escreverData("Saída", HOJE.plusDays(3));
        robo.clicar("Consultar");
        assertEquals(4, robo.linhasDaTabela());
        robo.capturar("disponibilidade");
        robo.escreverData("Saída", HOJE);
        robo.clicar("Consultar");
        assertEquals("Período inválido: a saída deve ser posterior à entrada.", robo.aviso());

        assertEquals("FAM", consultar("SELECT t.codigo FROM quarto q JOIN tipo_quarto t ON t.id = q.tipo_quarto_id "
                + "WHERE q.numero = 401"));
    }

    @Test
    void recepcionistaRegistraReservaPelaTela() {
        entrar("recep01", "Recep@123");
        robo.clicar("Registrar reserva");
        robo.escreverNoCampoComDica("CPF ou passaporte", "222.222.222-22");
        robo.clicar("Buscar hóspede");
        assertTrue(robo.exibeTexto("Beatriz Teste Souza"));
        robo.escolher("Tipo de quarto", t -> t.toString().startsWith("STD"));
        robo.escreverData("Entrada prevista", HOJE.plusDays(1));
        robo.escreverData("Saída prevista", HOJE.plusDays(3));
        robo.escrever("Quantidade de hóspedes", "3");
        robo.clicar("Confirmar reserva");
        assertEquals("O tipo STD comporta 2 hóspede(s); são necessários 3.", robo.aviso());

        robo.escrever("Quantidade de hóspedes", "2");
        robo.clicar("Verificar disponibilidade");
        assertTrue(robo.exibeTexto("2 quarto(s) STD disponível(is)"));
        robo.clicar("Confirmar reserva");
        assertTrue(robo.aviso().startsWith("Reserva 1002 registrada"), robo.aviso());
        robo.capturar("reserva-registrada");

        Map<String, Object> reserva = jdbc.queryForMap("""
                SELECT r.situacao, f.matricula, h.cpf_ou_passaporte FROM reserva r
                JOIN funcionario f ON f.id = r.funcionario_id JOIN hospede h ON h.id = r.hospede_responsavel_id
                WHERE r.codigo = 1002""");
        assertEquals("CONFIRMADA", reserva.get("situacao"));
        assertEquals("recep01", reserva.get("matricula"));
        assertEquals("22222222222", reserva.get("cpf_ou_passaporte"));
    }

    @Test
    void checkInComReservaPelaTelaOcupaQuartoEAbreConta() {
        entrar("recep01", "Recep@123");
        robo.clicar("Check-in");
        robo.escrever("Código da reserva", "1001");
        robo.clicar("Iniciar check-in");
        assertTrue(robo.exibeTexto("Reserva 1001 (CONFIRMADA)"));

        robo.escreverNoCampoComDica("CPF ou passaporte do hóspede", "11111111111");
        robo.clicar("Adicionar hóspede");
        robo.escreverNoCampoComDica("CPF ou passaporte do hóspede", "11111111111");
        robo.clicar("Adicionar hóspede");
        assertEquals("Hóspede já informado: Carlos Teste Silva.", robo.aviso());
        robo.escreverNoCampoComDica("CPF ou passaporte do hóspede", "99999999999");
        robo.clicar("Adicionar hóspede");
        assertTrue(robo.aviso().startsWith("Hóspede não cadastrado: 99999999999."));
        robo.escreverNoCampoComDica("CPF ou passaporte do hóspede", "22222222222");
        robo.clicar("Adicionar hóspede");

        robo.clicar("Consultar quartos disponíveis");
        assertEquals(2, robo.linhasDaTabela());
        robo.selecionarLinha(0);
        robo.clicar("Selecionar quarto");
        assertTrue(robo.exibeTexto("Quarto: 201"));
        robo.capturar("check-in-resumo");
        robo.clicar("Confirmar check-in");
        assertTrue(robo.aviso().startsWith("Check-in realizado com sucesso. Hospedagem 5001 · quarto 201 ocupado · "
                + "conta nº 1 aberta."), robo.aviso());
        robo.capturar("check-in-confirmado");

        Map<String, Object> estado = jdbc.queryForMap("""
                SELECT q.situacao AS quarto, r.situacao AS reserva, c.situacao AS conta,
                       (SELECT COUNT(*) FROM hospedagem_hospede WHERE hospedagem_id = h.id) AS hospedes
                FROM hospedagem h JOIN quarto q ON q.id = h.quarto_id JOIN reserva r ON r.id = h.reserva_id
                JOIN conta_hospedagem c ON c.hospedagem_id = h.id WHERE h.codigo = 5001""");
        assertEquals("OCUPADO", estado.get("quarto"));
        assertEquals("CONVERTIDA", estado.get("reserva"));
        assertEquals("ABERTA", estado.get("conta"));
        assertEquals(2L, estado.get("hospedes"));

        robo.escrever("Código da reserva", "1001");
        robo.clicar("Iniciar check-in");
        assertEquals("Reserva 1001 está convertida e não permite check-in.", robo.aviso());
    }

    @Test
    void checkInSemReservaPelaTelaRespeitaCapacidade() {
        entrar("admin01", "Admin@123");
        robo.clicar("Check-in");
        robo.clicar("Sem reserva");
        robo.escrever("CPF/passaporte do responsável", "XP1234567");
        robo.escolher("Tipo de quarto", t -> t.toString().startsWith("STD"));
        robo.escreverData("Saída prevista", HOJE.plusDays(2));
        robo.clicar("Iniciar check-in");
        assertTrue(robo.exibeTexto("Sem reserva"));

        for (String documento : new String[]{"XP1234567", "44444444444", "33333333333"}) {
            robo.escreverNoCampoComDica("CPF ou passaporte do hóspede", documento);
            robo.clicar("Adicionar hóspede");
        }
        assertEquals("O tipo STD comporta 2 hóspede(s); são necessários 3.", robo.aviso());

        robo.clicar("Consultar quartos disponíveis");
        assertEquals(2, robo.linhasDaTabela()); // 103 está em manutenção
        robo.selecionarLinha(0);
        robo.clicar("Selecionar quarto");
        robo.clicar("Confirmar check-in");
        assertTrue(robo.aviso().startsWith("Check-in realizado com sucesso. Hospedagem 5001 · quarto 101"), robo.aviso());
        robo.capturar("check-in-sem-reserva");

        assertEquals("0", consultar("SELECT COUNT(*) FROM hospedagem WHERE reserva_id IS NOT NULL"));
        assertEquals("ABERTA", consultar("SELECT situacao FROM conta_hospedagem"));
        assertEquals("1", consultar("SELECT COUNT(*) FROM reserva"));
    }
}
