package hospedabem.controller;

import hospedabem.model.NegocioException;
import hospedabem.model.Reserva;
import hospedabem.model.SituacaoReserva;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC08 Registrar Reserva. */
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
}
