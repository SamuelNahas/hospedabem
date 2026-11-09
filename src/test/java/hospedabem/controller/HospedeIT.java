package hospedabem.controller;

import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import hospedabem.model.Reserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC06 Gerenciar Hóspede: persistência, permissões e integração com a reserva (UC08) e o check-in (UC10). */
class HospedeIT extends IntegracaoBase {
    private static final String CPF_NOVO = "52998224725";
    private static final String CPF_OUTRO = "12345678909";
    private static final String PASSAPORTE = "AB9876543";
    private static final LocalDate NASCIMENTO = LocalDate.of(1990, 5, 20);

    @Autowired ControladorHospede hospedes;

    private static Hospede novo(String documento, String nome) {
        return Hospede.informado(0, documento, nome, NASCIMENTO, "Rua Fictícia, 1", "Campo Grande", "MS",
                "(67) 90000-0009", nome.toLowerCase().replace(' ', '.') + "@exemplo.test");
    }

    /** Hóspede só com os campos obrigatórios: endereço, cidade, estado e e-mail ausentes. */
    private static Hospede minimo(String documento, String nome) {
        return Hospede.informado(0, documento, nome, NASCIMENTO, null, null, null, "(67) 90000-0009", null);
    }

    @Test
    void recepcionistaIncluiBuscaListaAlteraEExcluiHospede() {
        logarComo("recep01");
        int antes = hospedes.listar().size();
        Hospede incluido = hospedes.incluir(novo("529.982.247-25", "Marta Teste Nova"));
        assertTrue(incluido.id() > 0);
        assertEquals(CPF_NOVO, incluido.cpfOuPassaporte());
        assertEquals(antes + 1, hospedes.listar().size());

        // A busca encontra pelo documento com pontuação, pelo documento limpo e por parte do nome.
        assertEquals(incluido, hospedes.buscar("529.982.247-25").orElseThrow());
        assertEquals(List.of(incluido), hospedes.procurar("529.982.247-25"));
        assertEquals(List.of(incluido), hospedes.procurar(CPF_NOVO));
        assertEquals(List.of(incluido), hospedes.procurar("marta teste"));

        hospedes.alterar(Hospede.informado(incluido.id(), PASSAPORTE, "Marta Teste Alterada", NASCIMENTO,
                null, "Dourados", "MS", "(67) 90000-0010", null));
        assertTrue(hospedes.buscar(CPF_NOVO).isEmpty());
        Hospede alterada = hospedes.buscar(PASSAPORTE).orElseThrow();
        assertEquals("Marta Teste Alterada", alterada.nome());
        assertEquals("Dourados", alterada.cidade());
        assertFalse(alterada.ehCpf());

        hospedes.excluir(alterada);
        assertTrue(hospedes.buscar(PASSAPORTE).isEmpty());
        assertEquals(antes, hospedes.listar().size());
    }

    @Test
    void camposOpcionaisAusentesSaoGravadosComoNulos() {
        Hospede gravado = hospedes.incluir(minimo(CPF_NOVO, "Marta Sem Opcionais"));
        Hospede lido = hospedes.buscar(CPF_NOVO).orElseThrow();
        assertEquals(gravado, lido);
        assertEquals(1, contar("SELECT COUNT(*) FROM hospede WHERE id = ? AND endereco IS NULL "
                + "AND cidade IS NULL AND estado IS NULL AND email IS NULL", lido.id()));
    }

    @Test
    void cpfEPassaporteSaoAceitosEIdentificacaoInvalidaNaoChegaAoBanco() {
        Hospede nacional = hospedes.incluir(minimo(CPF_NOVO, "Marta Brasileira"));
        Hospede estrangeiro = hospedes.incluir(minimo(" ab 987/6543 ", "John Test Estrangeiro"));
        assertTrue(nacional.ehCpf());
        assertFalse(estrangeiro.ehCpf());
        assertEquals(PASSAPORTE, estrangeiro.cpfOuPassaporte());

        int quantidade = hospedes.listar().size();
        for (String invalido : new String[]{null, "", "529.982.247-26", "1234567890", "AB#9876543"}) {
            assertThrows(NegocioException.class, () -> hospedes.incluir(minimo(invalido, "Marta Inválida")));
        }
        assertEquals(quantidade, hospedes.listar().size());
    }

    @Test
    void identificacaoDuplicadaEhImpedidaNaInclusaoENaAlteracao() {
        Hospede marta = hospedes.incluir(minimo(CPF_NOVO, "Marta Teste"));
        Hospede joao = hospedes.incluir(minimo(CPF_OUTRO, "João Teste"));

        assertThrows(NegocioException.class, () -> hospedes.incluir(minimo("529.982.247-25", "Marta Clone")));
        // Também contra a carga inicial, que grava o documento sem pontuação.
        assertThrows(NegocioException.class, () -> hospedes.incluir(minimo(CARLOS, "Carlos Clone")));
        assertThrows(NegocioException.class, () -> hospedes.alterar(Hospede.informado(joao.id(), CPF_NOVO,
                "João Clone", NASCIMENTO, null, null, null, "(67) 90000-0009", null)));

        assertEquals(marta, hospedes.buscar(CPF_NOVO).orElseThrow());
        assertEquals(joao, hospedes.buscar(CPF_OUTRO).orElseThrow());
    }

    @Test
    void alteracaoEExclusaoDeHospedeInexistenteSaoRecusadas() {
        Hospede inexistente = Hospede.informado(99999, CPF_NOVO, "Marta Fantasma", NASCIMENTO, null, null, null,
                "(67) 90000-0009", null);
        assertTrue(hospedes.buscar(CPF_NOVO).isEmpty());
        assertThrows(NegocioException.class, () -> hospedes.alterar(inexistente));
        assertThrows(NegocioException.class, () -> hospedes.excluir(inexistente));
    }

    /** Confere todas as operações sem sessão autenticada; com sessão, os dois perfis são aceitos. */
    @Test
    void sessaoAusenteNaoPodeExecutarNenhumaOperacaoEOsDoisPerfisPodem() {
        Hospede marta = hospedes.incluir(minimo(CPF_NOVO, "Marta Teste"));
        sessao.encerrar();
        assertThrows(AcessoNegadoException.class, hospedes::listar);
        assertThrows(AcessoNegadoException.class, () -> hospedes.procurar("marta"));
        assertThrows(AcessoNegadoException.class, () -> hospedes.buscar(CPF_NOVO));
        assertThrows(AcessoNegadoException.class, () -> hospedes.incluir(minimo(CPF_OUTRO, "João Teste")));
        assertThrows(AcessoNegadoException.class, () -> hospedes.alterar(marta));
        assertThrows(AcessoNegadoException.class, () -> hospedes.excluir(marta));

        logarComo("recep01");
        assertEquals(marta, hospedes.buscar(CPF_NOVO).orElseThrow());
        Hospede joao = hospedes.incluir(minimo(CPF_OUTRO, "João Teste"));
        logarComo("admin01");
        assertEquals(joao, hospedes.buscar(CPF_OUTRO).orElseThrow());
        hospedes.excluir(joao);
    }

    /** Vínculo com reserva (UC08) e com hospedagem (UC10) impede a exclusão e preserva o histórico. */
    @Test
    void hospedeComReservaOuHospedagemNaoPodeSerExcluido() {
        Hospede responsavel = hospedes.incluir(minimo(CPF_NOVO, "Marta Responsável"));
        Reserva reserva = reservas.registrar(CPF_NOVO, "SUI", HOJE.plusDays(3), HOJE.plusDays(5), 1);
        NegocioException erro = assertThrows(NegocioException.class, () -> hospedes.excluir(responsavel));
        assertTrue(erro.getMessage().contains("não pode ser excluído"), erro.getMessage());
        assertTrue(hospedes.buscar(CPF_NOVO).isPresent());
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE codigo = ?", reserva.getCodigo()));

        Hospede acompanhante = hospedes.incluir(minimo(CPF_OUTRO, "João Acompanhante"));
        checkInSemReserva(tipos.buscar("STD").orElseThrow(), 101, 2, CPF_NOVO, CPF_OUTRO);
        assertThrows(NegocioException.class, () -> hospedes.excluir(acompanhante));
        assertEquals(1, contar("SELECT COUNT(*) FROM hospedagem_hospede WHERE hospede_id = ?", acompanhante.id()));
    }

    /** O hóspede recém-incluído serve à reserva e ao check-in sem depender da carga inicial. */
    @Test
    void hospedeNovoPodeSerUsadoNaReservaENoCheckIn() {
        hospedes.incluir(minimo(CPF_NOVO, "Marta Responsável"));
        hospedes.incluir(minimo(PASSAPORTE, "John Test Estrangeiro"));

        Reserva reserva = reservas.registrar("529.982.247-25", "LUX", HOJE, HOJE.plusDays(2), 2);
        assertEquals(CPF_NOVO, reserva.getResponsavel().cpfOuPassaporte());

        ControladorCheckIn c = checkIns.getObject();
        c.iniciarCheckIn(reserva.getCodigo());
        c.adicionarHospede(CPF_NOVO);
        c.adicionarHospede(" ab 987/6543 ");
        c.selecionarQuarto(c.consultarQuartosDisponiveis().get(0).getNumero());
        var hospedagem = c.confirmarCheckIn();
        assertEquals(2, contar("SELECT COUNT(*) FROM hospedagem_hospede WHERE hospedagem_id = ?",
                hospedagem.getId()));
        assertEquals("CONVERTIDA", situacaoReserva(reserva.getCodigo()));
    }

    /** A mensagem de hóspede ausente passa a orientar o cadastro, não mais a adiar para a iteração 2. */
    @Test
    void mensagemDeHospedeNaoCadastradoOrientaOCadastroEORecomecoDoAtendimento() {
        NegocioException naReserva = assertThrows(NegocioException.class,
                () -> reservas.registrar(CPF_NOVO, "STD", HOJE, HOJE.plusDays(1), 1));
        assertEquals("Hóspede não cadastrado: " + CPF_NOVO + ". Cadastre-o em \"Hóspedes\" (UC06) e registre a "
                + "reserva em seguida.", naReserva.getMessage());

        ControladorCheckIn c = checkIns.getObject();
        NegocioException noCheckIn = assertThrows(NegocioException.class,
                () -> c.iniciarCheckInSemReserva(CPF_NOVO, "STD", HOJE, HOJE.plusDays(1)));
        assertEquals("Hóspede responsável não cadastrado: " + CPF_NOVO
                + ". Regularize o cadastro em \"Hóspedes\" (UC06) e reinicie o check-in.", noCheckIn.getMessage());

        // Atividade E do UC10: regularizado o cadastro, o atendimento é reiniciado e conclui.
        hospedes.incluir(minimo(CPF_NOVO, "Marta Responsável"));
        ControladorCheckIn reiniciado = checkIns.getObject();
        reiniciado.iniciarCheckInSemReserva(CPF_NOVO, "STD", HOJE, HOJE.plusDays(1));
        reiniciado.adicionarHospede(CPF_NOVO);
        reiniciado.selecionarQuarto(reiniciado.consultarQuartosDisponiveis().get(0).getNumero());
        assertTrue(reiniciado.confirmarCheckIn().getId() > 0);
    }

    /**
     * A data de nascimento é conferida contra o dia corrente no service, na inclusão e também na
     * alteração: o formulário aceita qualquer data válida, então nada impede a digitação de uma
     * data futura se o service não conferir.
     */
    @Test
    void dataDeNascimentoFuturaEhRecusadaNaInclusaoENaAlteracao() {
        Hospede futura = Hospede.informado(0, CPF_NOVO, "Marta Do Futuro", HOJE.plusDays(1), null, null, null,
                "(67) 90000-0009", null);
        assertThrows(NegocioException.class, () -> hospedes.incluir(futura));
        assertTrue(hospedes.buscar(CPF_NOVO).isEmpty());

        Hospede gravada = hospedes.incluir(minimo(CPF_NOVO, "Marta Teste"));
        assertThrows(NegocioException.class, () -> hospedes.alterar(Hospede.informado(gravada.id(), CPF_NOVO,
                "Marta Teste", HOJE.plusDays(1), null, null, null, "(67) 90000-0009", null)));
        assertEquals(NASCIMENTO, hospedes.buscar(CPF_NOVO).orElseThrow().dataNascimento());
    }

    /** Atualiza um banco da iteração 1: os índices do UC06 entram sem apagar hóspedes nem reservas. */
    @Test
    void migracaoCriaOsIndicesDoCadastroSemPerderDados() {
        int reserva = reservas.registrar(DIEGO, "SUI", HOJE.plusDays(3), HOJE.plusDays(5), 1).getCodigo();
        int antes = hospedes.listar().size();
        jdbc.execute("DROP INDEX hospede_nome");
        assertFalse(inicializador.inicializarSeVazio());
        assertEquals(1, contar("SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'hospede_nome'"));
        assertEquals(antes, hospedes.listar().size());
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE codigo = ?", reserva));
    }
}
