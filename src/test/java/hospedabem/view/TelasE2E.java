package hospedabem.view;

import hospedabem.BancoDeTeste;
import hospedabem.controller.Sessao;
import hospedabem.controller.ControladorItemConsumo;
import javafx.scene.control.ButtonType;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(robo.desabilitado("Funcionários"));
        assertTrue(robo.desabilitado("Itens de consumo e serviços"));
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

    /** Confere pela tela validação, cadastro, busca, alteração e cancelamento ou confirmação da exclusão. */
    @Test
    void administradorGerenciaItensDeConsumoPelaTela() {
        entrar("admin01", "Admin@123");
        robo.clicar("Itens de consumo e serviços");
        robo.clicar("Excluir");
        assertEquals("Selecione um item na lista.", robo.aviso());
        robo.escrever("Código", "AGUA");
        robo.escrever("Descrição", "Água mineral");
        robo.escrever("Categoria", "Frigobar");
        robo.escrever("Preço (R$)", "invalido");
        robo.clicar("Salvar");
        assertTrue(robo.aviso().startsWith("Preço inválido."));
        robo.escrever("Preço (R$)", "0");
        robo.clicar("Salvar");
        assertEquals("Preço deve ser positivo.", robo.aviso());
        robo.escrever("Preço (R$)", "5,50");
        robo.clicar("Salvar");
        assertEquals("Item AGUA gravado.", robo.aviso());
        assertEquals(1, robo.linhasDaTabela());
        robo.escrever("Código", "agua");
        robo.escrever("Descrição", "Duplicado");
        robo.escrever("Categoria", "Frigobar");
        robo.escrever("Preço (R$)", "7");
        robo.clicar("Salvar");
        assertEquals("Já existe item com o código AGUA.", robo.aviso());
        robo.clicar("Buscar por código");
        robo.escrever("Descrição", "Água com gás");
        robo.escrever("Preço (R$)", "6.25");
        robo.clicar("Salvar");
        assertEquals("Item AGUA gravado.", robo.aviso());
        assertEquals(new java.math.BigDecimal("6.25"),
                contexto.getBean(ControladorItemConsumo.class).buscar("AGUA").orElseThrow().preco());
        robo.selecionarLinha(0);
        robo.capturar("itens-de-consumo");
        robo.clicarConfirmando("Excluir", ButtonType.NO);
        assertEquals(1, robo.linhasDaTabela());
        robo.clicarConfirmando("Excluir", ButtonType.YES);
        assertEquals("Item AGUA excluído.", robo.aviso());
        assertEquals(0, robo.linhasDaTabela());
        robo.escrever("Código", "NAOEXISTE");
        robo.clicar("Buscar por código");
        assertEquals("Nenhum item com o código NAOEXISTE.", robo.aviso());
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

    /** UC11 e UC12 pelas telas: lançar consumo, conferir a conta, pagar e liberar o quarto. */
    @Test
    void recepcionistaLancaConsumoEConcluiCheckOutPelasTelas() {
        jdbc.update("INSERT INTO item_consumo (codigo, descricao, categoria, preco) VALUES ('AGUA', 'Água', 'Frigobar', 5.50)");
        entrar("recep01", "Recep@123");
        robo.clicar("Check-in");
        robo.clicar("Sem reserva");
        robo.escrever("CPF/passaporte do responsável", "XP1234567");
        robo.escolher("Tipo de quarto", t -> t.toString().startsWith("STD"));
        robo.escreverData("Saída prevista", HOJE.plusDays(2));
        robo.clicar("Iniciar check-in");
        robo.escreverNoCampoComDica("CPF ou passaporte do hóspede", "XP1234567");
        robo.clicar("Adicionar hóspede");
        robo.clicar("Consultar quartos disponíveis");
        robo.selecionarLinha(0);
        robo.clicar("Selecionar quarto");
        robo.clicar("Confirmar check-in");

        robo.clicar("Lançar consumo");
        assertEquals(1, robo.linhasDaTabela());
        robo.selecionarLinha(0);
        robo.aguardar();
        robo.escolher("Item", i -> i.toString().startsWith("AGUA"));
        robo.escrever("Quantidade", "0");
        robo.clicar("Lançar");
        assertEquals("Quantidade deve ser maior que zero.", robo.aviso());
        robo.escrever("Quantidade", "2");
        robo.clicar("Lançar");
        assertEquals("Lançado: 2 × Água = R$ 11,00.", robo.aviso());
        assertEquals("11.00", consultar("SELECT valor_total FROM consumo"));
        robo.capturar("consumo-lancado");

        robo.clicar("Check-out");
        robo.selecionarLinha(0);
        robo.clicar("Conferir conta");
        assertTrue(robo.exibeTexto("Total a pagar: R$ 191,00"));
        assertEquals("OCUPADO", consultar("SELECT situacao FROM quarto WHERE numero = 101"));
        robo.escolher("Forma", f -> f.toString().equals("Pix"));
        robo.clicarEConfirmar("Confirmar pagamento e check-out", ButtonType.YES);
        assertTrue(robo.aviso().startsWith("Check-out da hospedagem 5001 concluído"), robo.aviso());
        assertEquals(0, robo.linhasDaTabela());
        assertEquals("DISPONIVEL", consultar("SELECT situacao FROM quarto WHERE numero = 101"));
        assertEquals("PIX", consultar("SELECT forma FROM pagamento"));
        assertEquals("FECHADA", consultar("SELECT situacao FROM conta_hospedagem"));
        robo.capturar("check-out-concluido");
    }

    @Test
    void erroNaEtapaDoQuartoFicaVisivelMesmoComAPaginaRolada() {
        entrar("recep01", "Recep@123");
        robo.clicar("Check-in");
        robo.escrever("Código da reserva", "1001");
        robo.clicar("Iniciar check-in");
        robo.escreverNoCampoComDica("CPF ou passaporte do hóspede", "11111111111"); // digitado, mas não adicionado
        robo.clicar("Consultar quartos disponíveis");
        assertEquals("Informe ao menos um hóspede.", robo.aviso());
        assertTrue(robo.avisoForaDaRolagem());
        robo.capturar("erro-visivel-na-etapa-do-quarto");
    }

    @Test
    void administradorCadastraFuncionarioDelegaPerfilEORecemCadastradoEntraNoSistema() {
        entrar("admin01", "Admin@123");
        robo.clicar("Funcionários");
        assertEquals(2, robo.linhasDaTabela());
        // A data de contratação já vem com o dia corrente, e não em branco: o primeiro cadastro da
        // tela não pode falhar por um campo que o formulário recém-limpo preenche sozinho.
        assertEquals(Campos.texto(HOJE), robo.conteudo("Data de contratação"));

        // Inclusão com perfil de recepcionista e senha inicial.
        robo.escrever("Matrícula", "marta01");
        robo.escrever("CPF", "000.000.003-53");
        robo.escrever("Nome", "Marta Teste Nunes");
        robo.escrever("Endereço", "Rua Fictícia, 300");
        robo.escrever("Cidade", "Campo Grande");
        robo.escrever("Estado (UF)", "MS");
        robo.escrever("Telefone", "(67) 90000-0003");
        robo.escrever("E-mail", "marta@exemplo.test");
        robo.escolher("Perfil", p -> p.toString().equals("RECEPCIONISTA"));
        robo.escrever("Senha", "Marta@2026");
        robo.clicar("Salvar");
        assertEquals("Funcionário marta01 gravado como recepcionista.", robo.aviso());
        assertEquals(3, robo.linhasDaTabela());
        robo.capturar("funcionario-cadastrado");

        // Matrícula duplicada: mensagem compreensível e os dados digitados continuam no formulário.
        robo.escrever("Matrícula", "recep01");
        robo.escrever("CPF", "123.456.789-09");
        robo.escrever("Nome", "Bruno Teste Dias");
        robo.escreverData("Data de contratação", HOJE);
        robo.escrever("Senha", "Bruno@2026");
        robo.clicar("Salvar");
        assertEquals("Já existe funcionário com a matrícula recep01.", robo.aviso());
        assertEquals("Bruno Teste Dias", robo.conteudo("Nome"));
        assertEquals("123.456.789-09", robo.conteudo("CPF"));
        robo.capturar("funcionario-matricula-duplicada");

        // CPF duplicado, em outra grafia do mesmo número.
        robo.escrever("Matrícula", "bruno01");
        robo.escrever("CPF", "00000000353");
        robo.clicar("Salvar");
        assertEquals("Já existe funcionário com o CPF 000.000.003-53.", robo.aviso());

        // Delegação do perfil de administrador pela tela.
        robo.clicar("Limpar formulário");
        robo.escreverNoCampoComDica("Matrícula, nome ou CPF", "marta01");
        robo.clicar("Buscar funcionário");
        assertEquals(1, robo.linhasDaTabela());
        robo.selecionarLinha(0);
        robo.escolher("Perfil", p -> p.toString().equals("ADMINISTRADOR"));
        robo.clicar("Salvar");
        assertEquals("Funcionário marta01 gravado como administrador.", robo.aviso());
        assertEquals("ADMINISTRADOR", consultar("SELECT perfil FROM funcionario WHERE matricula = 'marta01'"));
        robo.capturar("funcionario-perfil-delegado");

        // Salvar não troca senha: com senha digitada numa alteração, a tela recusa em vez de gravar
        // o resto e deixar a senha antiga valendo sem avisar.
        robo.selecionarLinha(0);
        robo.escrever("Senha", "Outra@2026");
        robo.clicar("Salvar");
        assertTrue(robo.aviso().startsWith("Salvar não troca a senha."), robo.aviso());

        // Exclusão de quem tem reserva registrada é recusada, sem apagar o histórico.
        robo.escreverNoCampoComDica("Matrícula, nome ou CPF", "recep01");
        robo.clicar("Buscar funcionário");
        robo.selecionarLinha(0);
        robo.clicarEConfirmar("Excluir", ButtonType.YES);
        assertTrue(robo.aviso().contains("não pode ser excluído: há 1 reserva(s)/hospedagem(ns)"), robo.aviso());
        assertEquals("1", consultar("SELECT COUNT(*) FROM reserva WHERE situacao = 'CONFIRMADA'"));
        robo.capturar("funcionario-exclusao-com-vinculo");

        // Somente o hash é persistido e a nova administradora entra no sistema com matrícula e senha.
        assertEquals("1", consultar("SELECT COUNT(*) FROM funcionario WHERE matricula = 'marta01' "
                + "AND senha_hash LIKE 'pbkdf2$210000$%' AND senha_hash NOT LIKE '%Marta@2026%'"));
        robo.clicar("Sair");
        entrar("marta01", "Marta@2026");
        // RNF-8: a senha definida no cadastro é provisória; a troca é exigida antes do menu.
        assertTrue(robo.exibeTexto("Sua senha venceu ou foi definida pelo administrador. Defina uma nova senha para continuar."));
        robo.escrever("Senha atual", "Marta@2026");
        robo.escrever("Nova senha", "Propria2026");
        robo.escrever("Confirme a nova senha", "Outra2026");
        robo.clicar("Trocar senha");
        assertEquals("A confirmação não confere com a nova senha.", robo.aviso());
        robo.escrever("Senha atual", "Marta@2026");
        robo.escrever("Nova senha", "Propria2026");
        robo.escrever("Confirme a nova senha", "Propria2026");
        robo.clicar("Trocar senha");
        assertTrue(robo.exibeTexto("Administrador"));
        assertFalse(robo.desabilitado("Funcionários"));
        robo.clicar("Funcionários");
        assertEquals(3, robo.linhasDaTabela());
        robo.capturar("funcionario-recem-criado-autenticado");
    }
}
