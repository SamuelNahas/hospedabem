package hospedabem.controller;

import hospedabem.App;
import hospedabem.dao.FuncionarioDAO;
import hospedabem.dao.InicializadorBanco;
import hospedabem.model.Funcionario;
import hospedabem.model.NegocioException;
import hospedabem.model.Perfil;
import hospedabem.service.Senha;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC02 Gerenciar Funcionário (RF-2, RF-7, RF-12; RNF-7, RNF-10). */
class FuncionarioIT extends IntegracaoBase {
    private static final String CPF_MARTA = "000.000.003-53";
    private static final String CPF_BRUNO = "123.456.789-09";
    private static final char[] SENHA = "Marta@2026".toCharArray();

    private static char[] senha() {
        return "Marta@2026".toCharArray();
    }

    @Test
    void funcionarioEhIncluidoListadoBuscadoAlteradoEExcluido() {
        Funcionario marta = funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.RECEPCIONISTA), senha());
        assertTrue(marta.id() > 0);
        assertEquals(3, funcionarios.listar().size());
        assertEquals("Marta Teste marta01", funcionario("MARTA01 ").nome());

        funcionarios.alterar(new Funcionario(marta.id(), "marta01", CPF_MARTA, "Marta Teste Alterada", "Rua Nova, 2",
                "Dourados", "MS", "(67) 90000-0010", "nova@exemplo.test", HOJE.minusYears(2), Perfil.RECEPCIONISTA));
        Funcionario alterada = funcionario("marta01");
        assertEquals("Marta Teste Alterada", alterada.nome());
        assertEquals("Dourados", alterada.cidade());
        assertEquals(HOJE.minusYears(2), alterada.dataContratacao());

        funcionarios.excluir(alterada);
        assertTrue(acharFuncionario("marta01").isEmpty());
    }

    @Test
    void buscaPorMatriculaNomeOuCpfEncontraEOTermoDesconhecidoNaoRetornaNada() {
        funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.RECEPCIONISTA), senha());
        assertEquals(List.of("marta01"), funcionarios.procurar("MARTA01").stream().map(Funcionario::matricula).toList());
        assertEquals(1, funcionarios.procurar("Marta Teste").size());
        assertEquals(1, funcionarios.procurar("000.000.003-53").size());
        assertEquals(1, funcionarios.procurar("00000000353").size());
        assertEquals(3, funcionarios.procurar("").size());
        assertTrue(funcionarios.procurar("ninguem").isEmpty());
    }

    @Test
    void matriculaOuCpfDuplicadosSaoRecusadosNaInclusaoEEmAlteracoes() {
        Funcionario marta = funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.RECEPCIONISTA), senha());
        assertEquals("Já existe funcionário com a matrícula marta01.",
                assertThrows(NegocioException.class,
                        () -> funcionarios.incluir(novoFuncionario("MARTA01", CPF_BRUNO, Perfil.RECEPCIONISTA), senha()))
                        .getMessage());
        assertEquals("Já existe funcionário com o CPF " + CPF_MARTA + ".",
                assertThrows(NegocioException.class,
                        () -> funcionarios.incluir(novoFuncionario("bruno01", "00000000353", Perfil.RECEPCIONISTA), senha()))
                        .getMessage());

        Funcionario bruno = funcionarios.incluir(novoFuncionario("bruno01", CPF_BRUNO, Perfil.RECEPCIONISTA), senha());
        Funcionario conflito = new Funcionario(bruno.id(), "marta01", CPF_BRUNO, "Bruno Teste", null, null, null,
                null, null, HOJE, Perfil.RECEPCIONISTA);
        assertEquals("Já existe funcionário com a matrícula marta01.",
                assertThrows(NegocioException.class, () -> funcionarios.alterar(conflito)).getMessage());
        Funcionario conflitoCpf = new Funcionario(bruno.id(), "bruno01", CPF_MARTA, "Bruno Teste", null, null, null,
                null, null, HOJE, Perfil.RECEPCIONISTA);
        assertEquals("Já existe funcionário com o CPF " + CPF_MARTA + ".",
                assertThrows(NegocioException.class, () -> funcionarios.alterar(conflitoCpf)).getMessage());

        // Os dados de quem já estava gravado não foram tocados por nenhuma das recusas.
        assertEquals(4, funcionarios.listar().size());
        assertEquals("bruno01", funcionario("bruno01").matricula());
        assertEquals(CPF_MARTA, funcionario("marta01").cpf());
        assertEquals(marta.id(), funcionario("marta01").id());
    }

    @Test
    void campoInvalidoNaoChegaAoBanco() {
        assertThrows(NegocioException.class, () -> novoFuncionario("marta01", "111.111.111-11", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class,
                () -> funcionarios.incluir(new Funcionario(0, "marta01", CPF_MARTA, "Marta Teste", null, null, null,
                        null, null, HOJE.plusDays(1), Perfil.RECEPCIONISTA), senha()));
        assertThrows(NegocioException.class,
                () -> funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.RECEPCIONISTA), "curta1".toCharArray()));
        assertThrows(NegocioException.class,
                () -> funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.RECEPCIONISTA), null));
        assertEquals(2, funcionarios.listar().size());

        // A data de contratação também é conferida na alteração, não só na inclusão.
        Funcionario marta = funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.RECEPCIONISTA), senha());
        Funcionario futura = new Funcionario(marta.id(), "marta01", CPF_MARTA, "Marta Teste", null, null, null,
                null, null, HOJE.plusDays(1), Perfil.RECEPCIONISTA);
        assertThrows(NegocioException.class, () -> funcionarios.alterar(futura));
        assertEquals(HOJE.minusYears(1), funcionario("marta01").dataContratacao());
    }

    @Test
    void somenteOAdministradorGerenciaFuncionariosEDelegaOPerfil() {
        Funcionario marta = funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.RECEPCIONISTA), senha());
        logarComo("recep01");
        assertThrows(AcessoNegadoException.class, () -> funcionarios.listar());
        assertThrows(AcessoNegadoException.class, () -> funcionarios.procurar("marta"));
        assertThrows(AcessoNegadoException.class,
                () -> funcionarios.incluir(novoFuncionario("bruno01", CPF_BRUNO, Perfil.ADMINISTRADOR), senha()));
        Funcionario promovida = new Funcionario(marta.id(), "marta01", CPF_MARTA, "Marta Teste", null, null, null,
                null, null, HOJE, Perfil.ADMINISTRADOR);
        assertThrows(AcessoNegadoException.class, () -> funcionarios.alterar(promovida));
        assertThrows(AcessoNegadoException.class, () -> funcionarios.excluir(marta));
        assertThrows(AcessoNegadoException.class, () -> funcionarios.redefinirSenha(marta, senha()));

        sessao.encerrar();
        assertThrows(AcessoNegadoException.class, () -> funcionarios.listar());
        assertThrows(AcessoNegadoException.class,
                () -> funcionarios.incluir(novoFuncionario("bruno01", CPF_BRUNO, Perfil.ADMINISTRADOR), senha()));
        assertThrows(AcessoNegadoException.class, () -> funcionarios.alterar(promovida));
        assertThrows(AcessoNegadoException.class, () -> funcionarios.excluir(marta));
        assertThrows(AcessoNegadoException.class, () -> funcionarios.redefinirSenha(marta, senha()));

        logarComo("admin01");
        assertEquals(1, contar("SELECT COUNT(*) FROM funcionario WHERE perfil = 'ADMINISTRADOR'"));
        funcionarios.alterar(promovida);
        assertEquals(Perfil.ADMINISTRADOR, funcionario("marta01").perfil());
        assertEquals(2, contar("SELECT COUNT(*) FROM funcionario WHERE perfil = 'ADMINISTRADOR'"));
    }

    @Test
    void administradorNaoExcluiNemRebaixaOProprioCadastro() {
        Funcionario eu = funcionario("admin01");
        assertTrue(assertThrows(NegocioException.class, () -> funcionarios.excluir(eu))
                .getMessage().startsWith("Você não pode excluir o seu próprio cadastro"));
        Funcionario rebaixado = new Funcionario(eu.id(), eu.matricula(), eu.cpf(), eu.nome(), eu.endereco(),
                eu.cidade(), eu.estado(), eu.telefone(), eu.email(), eu.dataContratacao(), Perfil.RECEPCIONISTA);
        assertTrue(assertThrows(NegocioException.class, () -> funcionarios.alterar(rebaixado))
                .getMessage().startsWith("Você não pode rebaixar o seu próprio perfil"));
        assertEquals(Perfil.ADMINISTRADOR, funcionario("admin01").perfil());

        // Alterar os próprios dados sem mexer no perfil continua permitido.
        funcionarios.alterar(new Funcionario(eu.id(), eu.matricula(), eu.cpf(), "Ana Administradora Alterada",
                eu.endereco(), eu.cidade(), eu.estado(), eu.telefone(), eu.email(), eu.dataContratacao(),
                Perfil.ADMINISTRADOR));
        assertEquals("Ana Administradora Alterada", funcionario("admin01").nome());
    }

    /**
     * O perfil guardado na sessão é o de quem entrou. Se outra estação rebaixa ou exclui esse
     * cadastro, a sessão não pode continuar gerenciando funcionários: é essa reconferência que
     * garante que o hotel nunca fica sem nenhum administrador, porque quem rebaixa ou exclui um
     * administrador é sempre outro administrador conferido agora, e ninguém age sobre si mesmo.
     */
    @Test
    void aSessaoRebaixadaOuExcluidaPorOutraEstacaoPerdeOsPoderesDeAdministrador() {
        Funcionario admin01 = funcionario("admin01");
        Funcionario marta = funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.ADMINISTRADOR), senha());
        logarComo("marta01");
        jdbc.update("UPDATE funcionario SET perfil = 'RECEPCIONISTA' WHERE id = ?", marta.id());

        Funcionario rebaixado = Funcionario.informado(admin01.id(), admin01.matricula(), admin01.cpf(),
                admin01.nome(), null, null, null, null, null, admin01.dataContratacao(), Perfil.RECEPCIONISTA);
        for (Executable operacao : List.<Executable>of(
                () -> funcionarios.excluir(admin01),
                () -> funcionarios.alterar(rebaixado),
                () -> funcionarios.incluir(novoFuncionario("bruno01", CPF_BRUNO, Perfil.ADMINISTRADOR), senha()),
                () -> funcionarios.redefinirSenha(admin01, "Nova@Senha9".toCharArray())))
            assertTrue(assertThrows(NegocioException.class, operacao).getMessage()
                    .startsWith("Sua sessão não tem mais perfil de administrador"));

        // O mesmo vale para a sessão cujo cadastro foi excluído, não só rebaixado.
        jdbc.update("DELETE FROM funcionario WHERE id = ?", marta.id());
        assertThrows(NegocioException.class, () -> funcionarios.excluir(admin01));

        logarComo("admin01");
        assertEquals(Perfil.ADMINISTRADOR, funcionario("admin01").perfil());
        assertEquals(1, contar("SELECT COUNT(*) FROM funcionario WHERE perfil = 'ADMINISTRADOR'"));
        assertEquals(2, funcionarios.listar().size());
    }

    @Test
    void aMatriculaEhGravadaEmMinusculasEOLoginAceitaQualquerCaixa() {
        funcionarios.incluir(novoFuncionario("Maria01", CPF_MARTA, Perfil.RECEPCIONISTA), senha());
        assertEquals("maria01", funcionario("maria01").matricula());
        sessao.encerrar();
        assertEquals("maria01", autenticacao.autenticar("Maria01", senha()).matricula());
        assertEquals("maria01", autenticacao.autenticar(" MARIA01 ", senha()).matricula());
    }

    /**
     * Uma linha inserida por SQL na iteração 1, fora do formato que o UC02 passou a exigir, continua
     * sendo listada e continua autenticando: nenhum banco já populado precisa ser migrado.
     */
    @Test
    void linhaGravadaForaDoFormatoAtualAindaEhListadaEAutentica() {
        jdbc.update("INSERT INTO funcionario (matricula, cpf, nome, endereco, cidade, estado, telefone, email, "
                + "data_contratacao, perfil, senha_hash) VALUES "
                + "('adm_02', '00000000272', 'Jo', NULL, NULL, 'XX', '9000-0001', 'sem-arroba', ?, "
                + "'ADMINISTRADOR', ?)", HOJE.minusYears(2), Senha.gerarHash("Antiga@2026".toCharArray()));

        assertEquals(3, funcionarios.listar().size());
        assertEquals("9000-0001", funcionario("adm_02").telefone());
        sessao.encerrar();
        assertEquals(Perfil.ADMINISTRADOR, autenticacao.autenticar("adm_02", "Antiga@2026".toCharArray()).perfil());

        // Mas gravar essa linha de novo pela tela exige o formato atual.
        logarComo("admin01");
        Funcionario antigo = funcionario("adm_02");
        assertThrows(NegocioException.class, () -> funcionarios.alterar(antigo));
    }

    @Test
    void campoMaiorQueAColunaEhRecusadoComAMensagemDoCampo() {
        Funcionario longo = new Funcionario(0, "marta01", CPF_MARTA, "Marta Teste", "a".repeat(201), null, null,
                null, null, HOJE, Perfil.RECEPCIONISTA);
        assertEquals("Endereço deve ter no máximo 200 caracteres.",
                assertThrows(NegocioException.class, () -> funcionarios.incluir(longo, senha())).getMessage());
        assertEquals(2, funcionarios.listar().size());
    }

    @Test
    void outroAdministradorPodeExcluirOuRebaixarUmAdministradorSemVinculos() {
        Funcionario admin01 = funcionario("admin01");
        funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.ADMINISTRADOR), senha());
        logarComo("marta01");
        funcionarios.alterar(new Funcionario(admin01.id(), admin01.matricula(), admin01.cpf(), admin01.nome(),
                null, null, null, null, null, admin01.dataContratacao(), Perfil.RECEPCIONISTA));
        assertEquals(Perfil.RECEPCIONISTA, funcionario("admin01").perfil());
        funcionarios.excluir(funcionario("admin01"));
        assertTrue(acharFuncionario("admin01").isEmpty());
    }

    @Test
    void funcionarioComReservaNaoEhExcluidoEOHistoricoEhPreservado() {
        Funcionario recep = funcionario("recep01"); // registrou a reserva 1001 da carga
        assertTrue(assertThrows(NegocioException.class, () -> funcionarios.excluir(recep)).getMessage()
                .contains("não pode ser excluído: há 1 reserva(s)/hospedagem(ns)"));
        assertEquals(1, contar("SELECT COUNT(*) FROM funcionario WHERE matricula = 'recep01'"));
        assertEquals("CONFIRMADA", situacaoReserva(1001));
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE funcionario_id = ?", recep.id()));
    }

    @Test
    void funcionarioComHospedagemNaoEhExcluido() {
        Funcionario marta = funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.RECEPCIONISTA), senha());
        var std = tipos.buscar("STD").orElseThrow();
        logarComo("marta01");
        checkInSemReserva(std, 101, 2, CARLOS);
        logarComo("admin01");
        assertTrue(assertThrows(NegocioException.class, () -> funcionarios.excluir(marta)).getMessage()
                .contains("O histórico é preservado"));
        assertEquals(1, contar("SELECT COUNT(*) FROM hospedagem WHERE funcionario_id = ?", marta.id()));
    }

    @Test
    void alteracaoOuExclusaoDeFuncionarioInexistenteEhRecusada() {
        Funcionario fantasma = new Funcionario(99999, "fantasma1", CPF_BRUNO, "Fantasma Teste", null, null, null,
                null, null, HOJE, Perfil.RECEPCIONISTA);
        assertThrows(NegocioException.class, () -> funcionarios.alterar(fantasma));
        assertThrows(NegocioException.class, () -> funcionarios.excluir(fantasma));
        assertThrows(NegocioException.class, () -> funcionarios.redefinirSenha(fantasma, senha()));
    }

    @Test
    void somenteOHashEhPersistidoEOFuncionarioCriadoAutenticaComMatriculaESenha() {
        Funcionario marta = funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.ADMINISTRADOR), senha());
        assertEquals(0, contar("SELECT COUNT(*) FROM funcionario WHERE matricula = 'marta01' AND "
                + "(senha_hash NOT LIKE 'pbkdf2$210000$%' OR senha_hash LIKE '%Marta@2026%')"));

        sessao.encerrar();
        assertEquals(Perfil.ADMINISTRADOR, autenticacao.autenticar("marta01", senha()).perfil());
        assertThrows(NegocioException.class, () -> autenticacao.autenticar("marta01", "outra12345".toCharArray()));

        logarComo("admin01");
        funcionarios.redefinirSenha(marta, "Nova@Senha9".toCharArray());
        assertThrows(NegocioException.class, () -> funcionarios.redefinirSenha(marta, "curta1".toCharArray()));
        sessao.encerrar();
        assertThrows(NegocioException.class, () -> autenticacao.autenticar("marta01", senha()));
        assertEquals("marta01", autenticacao.autenticar("marta01", "Nova@Senha9".toCharArray()).matricula());
    }

    @Test
    void aSenhaEhApagadaDaMemoriaDepoisDeGravada() {
        char[] senha = senha();
        funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.RECEPCIONISTA), senha);
        assertEquals("\0".repeat(SENHA.length), new String(senha));
        char[] outra = "Nova@Senha9".toCharArray();
        funcionarios.redefinirSenha(funcionario("marta01"), outra);
        assertEquals("\0".repeat(11), new String(outra));
    }

    @Test
    void funcionarioGravadoSobreviveAoReinicioDaAplicacao() {
        Funcionario marta = funcionarios.incluir(novoFuncionario("marta01", CPF_MARTA, Perfil.ADMINISTRADOR), senha());
        try (ConfigurableApplicationContext reiniciado = new SpringApplicationBuilder(App.class)
                .profiles("test").headless(true).run()) {
            assertFalse(reiniciado.getBean(InicializadorBanco.class).inicializarSeVazio()); // não recria nem apaga
            FuncionarioDAO.Credencial credencial = reiniciado.getBean(FuncionarioDAO.class)
                    .buscarCredencial("marta01").orElseThrow();
            assertEquals(marta.id(), credencial.funcionario().id());
            assertEquals(Perfil.ADMINISTRADOR, credencial.funcionario().perfil());
            assertEquals("Campo Grande", credencial.funcionario().cidade());
            assertEquals(Perfil.ADMINISTRADOR, reiniciado.getBean(ControladorAutenticacao.class)
                    .autenticar("marta01", senha()).perfil());
        }
    }
}
