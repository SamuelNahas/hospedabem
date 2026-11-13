package hospedabem.controller;

import hospedabem.model.Disponibilidade;
import hospedabem.model.NegocioException;
import hospedabem.model.Perfil;
import hospedabem.model.Reserva;
import hospedabem.model.SituacaoReserva;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC08 Registrar Reserva e UC09 Consultar, Alterar e Cancelar Reserva. */
class ReservaIT extends IntegracaoBase {

    @Test
    void reservaValidaGeraCodigoERegistraFuncionarioDaSessao() {
        logarComo("recep01");
        Reserva r = reservas.registrar("111.111.111-11", "STD", HOJE.plusDays(1), HOJE.plusDays(3), 2);
        assertTrue(r.getCodigo() > 1001);
        assertEquals(SituacaoReserva.CONFIRMADA, r.getSituacao());
        assertEquals(sessao.getFuncionario().id(), r.getFuncionarioId());
        assertEquals(CARLOS, r.getResponsavel().cpfOuPassaporte());
    }

    @Test
    void buscarResponsavelRetornaOHospedeCadastrado() {
        assertEquals(CARLOS, reservas.buscarResponsavel(CARLOS).cpfOuPassaporte());
        assertThrows(NegocioException.class, () -> reservas.buscarResponsavel("99999999999"));
    }

    @Test
    void quantidadeAcimaDaCapacidadeOuNaoPositivaEhRecusada() {
        assertThrows(NegocioException.class, () -> reservas.registrar(CARLOS, "STD", HOJE, HOJE.plusDays(1), 3));
        assertThrows(NegocioException.class, () -> reservas.registrar(CARLOS, "STD", HOJE, HOJE.plusDays(1), 0));
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva"));
    }

    @Test
    void semDisponibilidadeEhRecusada() {
        TipoQuarto t = criarTipo("SO1", 2);
        criarQuarto(701, t);
        reservas.registrar(CARLOS, "SO1", HOJE.plusDays(1), HOJE.plusDays(4), 1);
        assertThrows(NegocioException.class, () -> reservas.registrar(BEATRIZ, "SO1", HOJE.plusDays(3), HOJE.plusDays(5), 1));
        reservas.registrar(BEATRIZ, "SO1", HOJE.plusDays(4), HOJE.plusDays(5), 1); // adjacente é aceito
    }

    @Test
    void responsavelTipoEPeriodoInvalidosSaoRecusados() {
        assertThrows(NegocioException.class, () -> reservas.registrar("99999999999", "STD", HOJE, HOJE.plusDays(1), 1));
        assertThrows(NegocioException.class, () -> reservas.registrar(CARLOS, "NADA", HOJE, HOJE.plusDays(1), 1));
        assertThrows(NegocioException.class, () -> reservas.registrar(CARLOS, "STD", HOJE.plusDays(2), HOJE.plusDays(1), 1));
        assertThrows(NegocioException.class, () -> reservas.registrar(CARLOS, "STD", HOJE.minusDays(1), HOJE.plusDays(1), 1));
    }

    // ----------------------------------------------------------------- UC09

    static final int RESERVA_CARGA = 1001; // LUX, hoje a hoje+3, Carlos, 2 hóspedes

    int versao(int codigo) {
        return contar("SELECT versao FROM reserva WHERE codigo = ?", codigo);
    }

    int livres(String codigoTipo, int deDias, int ateDias) {
        return disponibilidade.consultar(HOJE.plusDays(deDias), HOJE.plusDays(ateDias), codigoTipo).stream()
                .map(Disponibilidade::livres).findFirst().orElseThrow();
    }

    /** Tipo com um único quarto e uma reserva ocupando-o: a vaga disputada é a da própria reserva. */
    Reserva reservaUnicaDoTipo(String codigo) {
        TipoQuarto t = criarTipo(codigo, 3);
        criarQuarto(800 + t.id(), t);
        return reservas.registrar(CARLOS, codigo, HOJE.plusDays(1), HOJE.plusDays(4), 1);
    }

    @Test
    void consultaApresentaOsDadosEASituacaoDaReservaEhRecusaCodigoInexistente() {
        Reserva r = reservas.consultar(RESERVA_CARGA);
        assertEquals(SituacaoReserva.CONFIRMADA, r.getSituacao());
        assertEquals(CARLOS, r.getResponsavel().cpfOuPassaporte());
        assertEquals("LUX", r.getTipo().codigo());
        assertEquals(HOJE, r.getPeriodo().entrada());
        assertEquals(HOJE.plusDays(3), r.getPeriodo().saida());
        assertEquals(2, r.getQuantidadeHospedes());
        assertEquals("Reserva não localizada: 424242.",
                assertThrows(NegocioException.class, () -> reservas.consultar(424242)).getMessage());
    }

    @Test
    void alteracaoGravaResponsavelDatasEQuantidadeEIncrementaAVersao() {
        logarComo("recep01");
        Reserva antes = reservas.consultar(RESERVA_CARGA);
        assertEquals(0, versao(RESERVA_CARGA));

        Reserva depois = reservas.alterar(antes, BEATRIZ, "LUX", HOJE.plusDays(1), HOJE.plusDays(5), 3);
        assertEquals(BEATRIZ, depois.getResponsavel().cpfOuPassaporte());
        assertEquals(HOJE.plusDays(1), depois.getPeriodo().entrada());
        assertEquals(HOJE.plusDays(5), depois.getPeriodo().saida());
        assertEquals(3, depois.getQuantidadeHospedes());
        assertEquals(SituacaoReserva.CONFIRMADA, depois.getSituacao());
        assertEquals(1, versao(RESERVA_CARGA));
        // A linha é alterada, não recriada: mesmo id, mesmo código e mesmo funcionário que registrou.
        assertEquals(antes.getId(), depois.getId());
        assertEquals(antes.getFuncionarioId(), depois.getFuncionarioId());
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva"));
    }

    /** A vaga que a reserva já ocupa continua sendo dela: o recálculo desconsidera a própria reserva. */
    @Test
    void alteracaoMantemAPropriaVagaDoTipoComUmSoQuarto() {
        Reserva r = reservaUnicaDoTipo("UNI");
        assertEquals(0, livres("UNI", 1, 4)); // o tipo está lotado pela própria reserva

        Reserva depois = reservas.alterar(r, CARLOS, "UNI", HOJE.plusDays(2), HOJE.plusDays(6), 2);
        assertEquals(HOJE.plusDays(6), depois.getPeriodo().saida());
        assertEquals(2, depois.getQuantidadeHospedes());
        // Prorrogar de novo, agora pela reserva relida, continua valendo.
        assertEquals(3, reservas.alterar(depois, CARLOS, "UNI", HOJE.plusDays(2), HOJE.plusDays(6), 3)
                .getQuantidadeHospedes());
        assertEquals(2, versao(r.getCodigo()));
    }

    @Test
    void mudancaDeTipoConsomeVagaNoDestinoEDevolveCapacidadeNaOrigem() {
        Reserva r = reservaUnicaDoTipo("ORI");
        TipoQuarto destino = criarTipo("DES", 2);
        criarQuarto(900, destino);
        assertEquals(0, livres("ORI", 1, 4));
        assertEquals(1, livres("DES", 1, 4));

        Reserva depois = reservas.alterar(r, CARLOS, "DES", HOJE.plusDays(1), HOJE.plusDays(4), 2);
        assertEquals("DES", depois.getTipo().codigo());
        assertEquals(1, livres("ORI", 1, 4)); // a origem devolveu a capacidade
        assertEquals(0, livres("DES", 1, 4)); // o destino consumiu a vaga
        assertEquals("DES", jdbc.queryForObject("SELECT t.codigo FROM reserva r JOIN tipo_quarto t "
                + "ON t.id = r.tipo_quarto_id WHERE r.codigo = ?", String.class, depois.getCodigo()));
    }

    @Test
    void alteracaoSemVagaNoTipoDeDestinoOuNoPeriodoNovoEhRecusadaSemGravar() {
        Reserva r = reservaUnicaDoTipo("CHE");
        TipoQuarto lotado = criarTipo("LOT", 2);
        criarQuarto(901, lotado);
        reservas.registrar(BEATRIZ, "LOT", HOJE.plusDays(1), HOJE.plusDays(4), 1); // ocupa o único quarto

        assertThrows(NegocioException.class, () -> reservas.alterar(r, CARLOS, "LOT", HOJE.plusDays(2),
                HOJE.plusDays(3), 1));
        // Nada foi gravado: a reserva segue no tipo e no período originais, com a versão intacta.
        Reserva atual = reservas.consultar(r.getCodigo());
        assertEquals("CHE", atual.getTipo().codigo());
        assertEquals(HOJE.plusDays(4), atual.getPeriodo().saida());
        assertEquals(0, versao(r.getCodigo()));

        // Prorrogar a própria reserva sobre o período de outra não cabe no único quarto do tipo:
        // desconsiderar a própria reserva não dispensa a vaga no período novo.
        reservas.registrar(BEATRIZ, "CHE", HOJE.plusDays(5), HOJE.plusDays(7), 1);
        assertThrows(NegocioException.class, () -> reservas.alterar(atual, CARLOS, "CHE", HOJE.plusDays(1),
                HOJE.plusDays(8), 1));
        assertEquals(HOJE.plusDays(4), reservas.consultar(r.getCodigo()).getPeriodo().saida());
    }

    @Test
    void cancelamentoPersisteCanceladaPreservaORegistroEDevolveCapacidade() {
        Reserva r = reservaUnicaDoTipo("CAN");
        logarComo("recep01"); // o cancelamento é da recepção, não só do administrador
        assertEquals(0, livres("CAN", 1, 4));

        Reserva cancelada = reservas.cancelar(reservas.consultar(r.getCodigo()));
        assertEquals(SituacaoReserva.CANCELADA, cancelada.getSituacao());
        assertEquals("CANCELADA", situacaoReserva(r.getCodigo()));
        assertEquals(1, versao(r.getCodigo()));
        assertEquals(1, livres("CAN", 1, 4)); // a capacidade voltou à consulta de disponibilidade (UC07)
        // Nada é apagado: a linha continua com o responsável, o tipo e o período que tinha.
        assertEquals(CARLOS, cancelada.getResponsavel().cpfOuPassaporte());
        assertEquals(HOJE.plusDays(4), cancelada.getPeriodo().saida());
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE codigo = ?", r.getCodigo()));
        // E a vaga devolvida serve a outra reserva.
        assertEquals(SituacaoReserva.CONFIRMADA,
                reservas.registrar(BEATRIZ, "CAN", HOJE.plusDays(1), HOJE.plusDays(4), 1).getSituacao());
    }

    /** A reserva convertida em hospedagem (UC10) não volta atrás: nem alteração, nem cancelamento. */
    @Test
    void reservaConvertidaNaoEhAlteradaNemCancelada() {
        Reserva antes = reservas.consultar(RESERVA_CARGA);
        ControladorCheckIn c = checkIns.getObject();
        c.iniciarCheckIn(RESERVA_CARGA);
        c.adicionarHospede(CARLOS);
        c.selecionarQuarto(201);
        c.confirmarCheckIn();

        Reserva convertida = reservas.consultar(RESERVA_CARGA);
        assertEquals(SituacaoReserva.CONVERTIDA, convertida.getSituacao());
        assertTrue(assertThrows(NegocioException.class, () -> reservas.alterar(convertida, CARLOS, "LUX",
                HOJE, HOJE.plusDays(5), 2)).getMessage().contains("está convertida"));
        assertTrue(assertThrows(NegocioException.class, () -> reservas.cancelar(convertida))
                .getMessage().contains("está convertida"));
        assertEquals("CONVERTIDA", situacaoReserva(RESERVA_CARGA));
        // A reserva como estava antes do check-in também é recusada, agora por estar desatualizada.
        assertTrue(assertThrows(NegocioException.class, () -> reservas.cancelar(antes))
                .getMessage().contains("foi alterada por outra operação"));
    }

    /**
     * Reserva já cancelada: a decisão da equipe é recusar, e não tratar o cancelamento como
     * idempotente, porque a tela mostra a situação antes de agir — repetir a ação significa que a
     * tela está desatualizada, e a mensagem diz isso.
     */
    @Test
    void reservaJaCanceladaNaoEhAlteradaNemCanceladaDeNovo() {
        Reserva cancelada = reservas.cancelar(reservas.consultar(RESERVA_CARGA));
        assertTrue(assertThrows(NegocioException.class, () -> reservas.cancelar(cancelada))
                .getMessage().contains("está cancelada"));
        assertTrue(assertThrows(NegocioException.class, () -> reservas.alterar(cancelada, CARLOS, "LUX",
                HOJE, HOJE.plusDays(5), 2)).getMessage().contains("está cancelada"));
        assertEquals("CANCELADA", situacaoReserva(RESERVA_CARGA));
        assertEquals(1, versao(RESERVA_CARGA)); // a recusa não incrementa a versão
    }

    /** O formulário aberto antes da alteração de outra estação não a sobrescreve (perda de atualização). */
    @Test
    void alteracaoOuCancelamentoSobreReservaDesatualizadaEhRecusado() {
        Reserva aberta = reservas.consultar(RESERVA_CARGA);
        reservas.alterar(aberta, CARLOS, "LUX", HOJE, HOJE.plusDays(4), 2); // outra estação altera

        assertTrue(assertThrows(NegocioException.class, () -> reservas.alterar(aberta, BEATRIZ, "LUX",
                HOJE, HOJE.plusDays(3), 2)).getMessage().contains("Localize-a de novo e refaça a alteração"));
        assertTrue(assertThrows(NegocioException.class, () -> reservas.cancelar(aberta)).getMessage()
                .contains("confirme o cancelamento de novo"));
        assertEquals(1, versao(RESERVA_CARGA));
        assertEquals(HOJE.plusDays(4), reservas.consultar(RESERVA_CARGA).getPeriodo().saida());
    }

    @Test
    void alteracaoComResponsavelTipoPeriodoOuQuantidadeInvalidosEhRecusada() {
        Reserva r = reservas.consultar(RESERVA_CARGA);
        assertThrows(NegocioException.class, () -> reservas.alterar(r, "99999999999", "LUX", HOJE, HOJE.plusDays(4), 2));
        assertThrows(NegocioException.class, () -> reservas.alterar(r, CARLOS, "NADA", HOJE, HOJE.plusDays(4), 2));
        assertThrows(NegocioException.class, () -> reservas.alterar(r, CARLOS, "LUX", HOJE.plusDays(4), HOJE.plusDays(2), 2));
        assertThrows(NegocioException.class, () -> reservas.alterar(r, CARLOS, "LUX", HOJE.minusDays(1), HOJE.plusDays(4), 2));
        assertThrows(NegocioException.class, () -> reservas.alterar(r, CARLOS, "LUX", HOJE, HOJE.plusDays(4), 4));
        assertThrows(NegocioException.class, () -> reservas.alterar(r, CARLOS, "LUX", HOJE, HOJE.plusDays(4), 0));
        // Sem nenhuma mudança também é recusado, para não subir a versão e derrubar um check-in em curso.
        assertTrue(assertThrows(NegocioException.class, () -> reservas.alterar(r, CARLOS, "LUX", HOJE,
                HOJE.plusDays(3), 2)).getMessage().contains("os dados informados são os já gravados"));
        assertEquals(0, versao(RESERVA_CARGA));
    }

    /** Hóspede excluído por outra estação entre a consulta e a gravação da alteração. */
    @Test
    void responsavelExcluidoPorOutraEstacaoImpedeAAlteracao() {
        Reserva r = reservas.consultar(RESERVA_CARGA);
        jdbc.update("DELETE FROM hospede WHERE cpf_ou_passaporte = ?", ELISA);
        assertThrows(NegocioException.class, () -> reservas.alterar(r, ELISA, "LUX", HOJE, HOJE.plusDays(4), 2));
        assertEquals(0, versao(RESERVA_CARGA));
    }

    /** Linha apagada por fora entre a consulta da tela e a gravação: a operação diz que não a achou. */
    @Test
    void reservaApagadaEntreAConsultaEAGravacaoEhInformada() {
        Reserva r = reservas.consultar(RESERVA_CARGA);
        jdbc.update("DELETE FROM reserva WHERE codigo = ?", RESERVA_CARGA);
        assertEquals("Reserva não localizada: 1001.",
                assertThrows(NegocioException.class, () -> reservas.cancelar(r)).getMessage());
        assertEquals("Reserva não localizada: 1001.",
                assertThrows(NegocioException.class, () -> reservas.alterar(r, CARLOS, "LUX", HOJE,
                        HOJE.plusDays(5), 2)).getMessage());
    }

    @Test
    void osDoisPerfisConsultamAlteramECancelamESemSessaoNadaEhPermitido() {
        Reserva r = reservas.consultar(RESERVA_CARGA);
        sessao.encerrar();
        assertThrows(AcessoNegadoException.class, () -> reservas.consultar(RESERVA_CARGA));
        assertThrows(AcessoNegadoException.class, () -> reservas.alterar(r, CARLOS, "LUX", HOJE, HOJE.plusDays(4), 2));
        assertThrows(AcessoNegadoException.class, () -> reservas.cancelar(r));
        assertEquals(0, versao(RESERVA_CARGA));

        logarComo("recep01");
        Reserva pelaRecepcionista = reservas.alterar(reservas.consultar(RESERVA_CARGA), CARLOS, "LUX", HOJE,
                HOJE.plusDays(4), 2);
        assertEquals(Perfil.RECEPCIONISTA, perfilDaSessao());
        logarComo("admin01");
        assertEquals(Perfil.ADMINISTRADOR, perfilDaSessao());
        assertEquals(SituacaoReserva.CANCELADA, reservas.cancelar(pelaRecepcionista).getSituacao());
    }
}
