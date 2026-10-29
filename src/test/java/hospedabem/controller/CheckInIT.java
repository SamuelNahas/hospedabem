package hospedabem.controller;

import hospedabem.model.Hospedagem;
import hospedabem.model.NegocioException;
import hospedabem.model.SituacaoConta;
import hospedabem.model.SituacaoReserva;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC10 Realizar Check-in e contrato de confirmarCheckIn(). */
class CheckInIT extends IntegracaoBase {
    static final int RESERVA_CARGA = 1001; // LUX, hoje a hoje+3, Carlos, 2 hóspedes

    ControladorCheckIn ateResumo(int codigoReserva, int quarto, String... documentos) {
        ControladorCheckIn c = checkIns.getObject();
        c.iniciarCheckIn(codigoReserva);
        for (String d : documentos) c.adicionarHospede(d);
        assertTrue(c.consultarQuartosDisponiveis().stream().anyMatch(q -> q.getNumero() == quarto));
        c.selecionarQuarto(quarto);
        return c;
    }

    void assertNadaGravado() {
        assertEquals(0, contar("SELECT COUNT(*) FROM hospedagem"));
        assertEquals(0, contar("SELECT COUNT(*) FROM hospedagem_hospede"));
        assertEquals(0, contar("SELECT COUNT(*) FROM conta_hospedagem"));
        assertEquals(0, contar("SELECT COUNT(*) FROM quarto WHERE situacao = 'OCUPADO'"));
        assertEquals("CONFIRMADA", situacaoReserva(RESERVA_CARGA));
    }

    @Test
    void checkInComReservaCumpreTodasAsPosCondicoesDoContrato() {
        logarComo("recep01");
        Hospedagem h = ateResumo(RESERVA_CARGA, 201, CARLOS, BEATRIZ).confirmarCheckIn();

        assertTrue(h.getCodigo() >= 5001);
        assertEquals(SituacaoConta.ABERTA, h.getConta().getSituacao());
        assertEquals(SituacaoReserva.CONVERTIDA, h.getReserva().getSituacao());
        Map<String, Object> linha = jdbc.queryForMap("""
                SELECT h.situacao, h.data_saida_prevista, h.data_hora_entrada::date AS entrada, q.numero, q.situacao AS sq,
                       r.situacao AS sr, c.situacao AS sc, c.id AS conta, c.valor_diaria
                FROM hospedagem h JOIN quarto q ON q.id = h.quarto_id JOIN reserva r ON r.id = h.reserva_id
                JOIN conta_hospedagem c ON c.hospedagem_id = h.id WHERE h.codigo = ?""", h.getCodigo());
        assertEquals("ATIVA", linha.get("situacao"));
        assertEquals(java.sql.Date.valueOf(HOJE.plusDays(3)), linha.get("data_saida_prevista"));
        assertEquals(java.sql.Date.valueOf(HOJE), linha.get("entrada"));
        assertEquals(201, linha.get("numero"));
        assertEquals("OCUPADO", linha.get("sq"));
        assertEquals("CONVERTIDA", linha.get("sr"));
        assertEquals("ABERTA", linha.get("sc"));
        assertEquals(h.getConta().getId(), linha.get("conta"));
        assertEquals(0, new java.math.BigDecimal("320.00").compareTo((java.math.BigDecimal) linha.get("valor_diaria")));
        assertEquals(2, contar("SELECT COUNT(*) FROM hospedagem_hospede"));
        assertEquals(1, contar("SELECT COUNT(*) FROM conta_hospedagem"));
    }

    @Test
    void checkInSemReservaNaoCriaReservaIntermediaria() {
        TipoQuarto std = tipos.buscar("STD").orElseThrow();
        Hospedagem h = checkInSemReserva(std, 101, 2, "XP1234567", ELISA);
        assertNull(h.getReserva());
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva")); // só a da carga
        assertEquals(1, contar("SELECT COUNT(*) FROM hospedagem WHERE reserva_id IS NULL AND situacao = 'ATIVA'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM conta_hospedagem WHERE situacao = 'ABERTA'"));
        assertEquals("OCUPADO", situacaoQuarto(101));
    }

    @Test
    void reservaInexistenteConvertidaOuCanceladaNaoIniciaCheckIn() {
        ControladorCheckIn c = checkIns.getObject();
        assertThrows(NegocioException.class, () -> c.iniciarCheckIn(424242));
        ateResumo(RESERVA_CARGA, 201, CARLOS).confirmarCheckIn();
        assertThrows(NegocioException.class, () -> c.iniciarCheckIn(RESERVA_CARGA));
        int futura = reservas.registrar(BEATRIZ, "STD", HOJE.plusDays(3), HOJE.plusDays(4), 1).getCodigo();
        assertThrows(NegocioException.class, () -> c.iniciarCheckIn(futura)); // entrada ainda não chegou
        int cancelada = reservas.registrar(BEATRIZ, "STD", HOJE, HOJE.plusDays(1), 1).getCodigo();
        jdbc.update("UPDATE reserva SET situacao = 'CANCELADA' WHERE codigo = ?", cancelada);
        assertThrows(NegocioException.class, () -> c.iniciarCheckIn(cancelada));
    }

    @Test
    void variantesSemReservaValidamResponsavelTipoEPeriodo() {
        ControladorCheckIn c = checkIns.getObject();
        assertThrows(NegocioException.class, () -> c.iniciarCheckInSemReserva("00000000000", "STD", HOJE, HOJE.plusDays(1)));
        assertThrows(NegocioException.class, () -> c.iniciarCheckInSemReserva(CARLOS, "NADA", HOJE, HOJE.plusDays(1)));
        assertThrows(NegocioException.class, () -> c.iniciarCheckInSemReserva(CARLOS, "STD", HOJE, HOJE));
        assertThrows(NegocioException.class, () -> c.iniciarCheckInSemReserva(CARLOS, "STD", HOJE.plusDays(1), HOJE.plusDays(2)));
    }

    @Test
    void hospedeInexistenteDuplicadoOuAcimaDaCapacidadeEhRecusado() {
        ControladorCheckIn c = checkIns.getObject();
        c.iniciarCheckIn(RESERVA_CARGA); // LUX: capacidade 3
        assertThrows(NegocioException.class, () -> c.adicionarHospede("12345678900"));
        c.adicionarHospede(CARLOS);
        assertThrows(NegocioException.class, () -> c.adicionarHospede("111.111.111-11"));
        c.adicionarHospede(BEATRIZ);
        c.adicionarHospede(DIEGO);
        assertThrows(NegocioException.class, () -> c.adicionarHospede(ELISA));
        assertEquals(3, c.getParticipantes().size());
    }

    @Test
    void responsavelPrecisaEstarEntreOsParticipantes() {
        ControladorCheckIn c = checkIns.getObject();
        c.iniciarCheckIn(RESERVA_CARGA);
        c.adicionarHospede(BEATRIZ);
        assertThrows(NegocioException.class, c::consultarQuartosDisponiveis);
    }

    @Test
    void semQuartoDisponivelOuQuartoInadequadoEhInformado() {
        TipoQuarto t = criarTipo("ULT", 2);
        criarQuarto(601, t);
        checkInSemReserva(t, 601, 2, CARLOS);
        ControladorCheckIn c = checkIns.getObject();
        c.iniciarCheckInSemReserva(BEATRIZ, "ULT", HOJE, HOJE.plusDays(1));
        c.adicionarHospede(BEATRIZ);
        assertThrows(NegocioException.class, c::consultarQuartosDisponiveis);
        assertThrows(NegocioException.class, () -> c.selecionarQuarto(601)); // ocupado
        assertThrows(NegocioException.class, () -> c.selecionarQuarto(103)); // outro tipo
    }

    @Test
    void reservaAlteradaAposOResumoImpedeConfirmacao() {
        ControladorCheckIn c = ateResumo(RESERVA_CARGA, 201, CARLOS);
        jdbc.update("UPDATE reserva SET data_saida_prevista = data_saida_prevista + 1, versao = versao + 1 WHERE codigo = ?",
                RESERVA_CARGA); // alteração feita por outra estação (UC09)
        assertThrows(NegocioException.class, c::confirmarCheckIn);
        assertEquals(0, contar("SELECT COUNT(*) FROM hospedagem"));
        assertEquals("DISPONIVEL", situacaoQuarto(201));
    }

    @Test
    void reservaCanceladaAposOResumoImpedeConfirmacao() {
        ControladorCheckIn c = ateResumo(RESERVA_CARGA, 201, CARLOS);
        jdbc.update("UPDATE reserva SET situacao = 'CANCELADA', versao = versao + 1 WHERE codigo = ?", RESERVA_CARGA);
        assertThrows(NegocioException.class, c::confirmarCheckIn);
        assertEquals(0, contar("SELECT COUNT(*) FROM hospedagem"));
    }

    @Test
    void quartoOcupadoOuEmManutencaoAposOResumoImpedeConfirmacao() {
        ControladorCheckIn primeiro = ateResumo(RESERVA_CARGA, 201, CARLOS);
        TipoQuarto lux = tipos.buscar("LUX").orElseThrow();
        checkInSemReserva(lux, 201, 1, ELISA); // outra estação ocupa o mesmo quarto
        assertThrows(NegocioException.class, primeiro::confirmarCheckIn);
        assertEquals("CONFIRMADA", situacaoReserva(RESERVA_CARGA));

        ControladorCheckIn segundo = ateResumo(RESERVA_CARGA, 202, CARLOS);
        jdbc.update("UPDATE quarto SET situacao = 'MANUTENCAO' WHERE numero = 202");
        assertThrows(NegocioException.class, segundo::confirmarCheckIn);
        assertEquals(1, contar("SELECT COUNT(*) FROM hospedagem"));
    }

    @Test
    void falhaDuranteAGravacaoDesfazTudo() {
        jdbc.execute("""
                CREATE FUNCTION falha_simulada() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'falha simulada na gravação da conta'; END $$ LANGUAGE plpgsql""");
        jdbc.execute("CREATE TRIGGER falha_conta BEFORE INSERT ON conta_hospedagem FOR EACH ROW EXECUTE FUNCTION falha_simulada()");
        ControladorCheckIn c = ateResumo(RESERVA_CARGA, 201, CARLOS, BEATRIZ);
        assertThrows(RuntimeException.class, c::confirmarCheckIn);
        assertNadaGravado();

        jdbc.execute("DROP TRIGGER falha_conta ON conta_hospedagem");
        jdbc.execute("CREATE TRIGGER falha_reserva BEFORE UPDATE ON reserva FOR EACH ROW EXECUTE FUNCTION falha_simulada()");
        assertThrows(RuntimeException.class, c::confirmarCheckIn); // falha no último passo do contrato
        assertNadaGravado();
    }

    @Test
    void bancoImpedeSegundaContaEReservaConvertidaDuasVezes() {
        Hospedagem h = ateResumo(RESERVA_CARGA, 201, CARLOS).confirmarCheckIn();
        assertThrows(Exception.class, () -> jdbc.update(
                "INSERT INTO conta_hospedagem (hospedagem_id, situacao, valor_diaria) VALUES (?, 'ABERTA', 1)", h.getId()));
        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO hospedagem (data_hora_entrada, data_saida_prevista, situacao, quarto_id, reserva_id, funcionario_id)
                SELECT now(), CURRENT_DATE + 1, 'ATIVA', q.id, h.reserva_id, h.funcionario_id
                FROM quarto q, hospedagem h WHERE q.numero = 202 AND h.id = ?""", h.getId()));
    }
}
