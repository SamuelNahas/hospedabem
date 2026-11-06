package hospedabem.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static hospedabem.model.ConsumoDominioTest.AGUA;
import static hospedabem.model.ConsumoDominioTest.ENTRADA;
import static hospedabem.model.ConsumoDominioTest.hospedagem;
import static org.junit.jupiter.api.Assertions.*;

/** Regras do UC11 e do UC12 no modelo, sem banco. */
class CheckOutDominioTest {
    @Test
    void diariasContamNoitesComMinimoDeUma() {
        assertEquals(1, RegraDiarias.contar(ENTRADA, ENTRADA.plusHours(3)));            // mesmo dia
        assertEquals(1, RegraDiarias.contar(ENTRADA, LocalDateTime.of(2026, 11, 3, 0, 1)));
        assertEquals(3, RegraDiarias.contar(ENTRADA, LocalDateTime.of(2026, 11, 5, 23, 59))); // no prazo, sem acréscimo por horário
        assertEquals(1, RegraDiarias.contar(ENTRADA, LocalDateTime.of(2026, 11, 3, 10, 0)));  // saída antecipada
        assertEquals(5, RegraDiarias.contar(ENTRADA, LocalDateTime.of(2026, 11, 7, 18, 0)));  // atraso
        assertThrows(NegocioException.class, () -> RegraDiarias.contar(ENTRADA, ENTRADA.minusMinutes(1)));
    }

    @Test
    void totalSomaDiariasEConsumosSemPerderCentavos() {
        Hospedagem h = hospedagem();
        h.lancarConsumo(AGUA, 3, ENTRADA);
        h.lancarConsumo(new ItemConsumo(2, "LAV", "Lavanderia", "Serviço", new BigDecimal("0.10")), 7, ENTRADA);
        Extrato e = h.extrato(LocalDateTime.of(2026, 11, 4, 11, 0));
        assertEquals(2, e.quantidadeDiarias());
        assertEquals(new BigDecimal("360.00"), e.valorDiarias());
        assertEquals(new BigDecimal("17.20"), e.valorConsumos());
        assertEquals(new BigDecimal("377.20"), e.total());
    }

    @Test
    void encerrarFechaContaLiberaQuartoERecusaRepeticaoELancamento() {
        Hospedagem h = hospedagem();
        LocalDateTime saida = LocalDateTime.of(2026, 11, 4, 11, 0);
        Pagamento p = h.encerrar(FormaPagamento.PIX, new BigDecimal("360.00"), saida);
        assertEquals(FormaPagamento.PIX, p.forma());
        assertEquals(SituacaoHospedagem.ENCERRADA, h.getSituacao());
        assertEquals(SituacaoConta.FECHADA, h.getConta().getSituacao());
        assertEquals(SituacaoQuarto.DISPONIVEL, h.getQuarto().getSituacao());
        assertEquals(saida, h.getDataHoraSaida());
        assertThrows(NegocioException.class, () -> h.encerrar(FormaPagamento.PIX, new BigDecimal("360.00"), saida));
        assertThrows(NegocioException.class, () -> h.lancarConsumo(AGUA, 1, saida));
    }

    @Test
    void totalDivergenteOuFormaAusenteNaoEncerram() {
        Hospedagem h = hospedagem();
        LocalDateTime saida = LocalDateTime.of(2026, 11, 4, 11, 0);
        assertThrows(NegocioException.class, () -> h.encerrar(FormaPagamento.DINHEIRO, new BigDecimal("180.00"), saida));
        assertThrows(NegocioException.class, () -> h.encerrar(null, new BigDecimal("360.00"), saida));
        assertEquals(SituacaoHospedagem.ATIVA, h.getSituacao());
        assertEquals(SituacaoConta.ABERTA, h.getConta().getSituacao());
        assertEquals(SituacaoQuarto.OCUPADO, h.getQuarto().getSituacao());
    }

    @Test
    void confirmarSemConferirPedeAConferenciaETotalAcimaDoLimiteERecusado() {
        Hospedagem h = hospedagem();
        assertEquals("Confira o extrato antes de confirmar o pagamento.", assertThrows(NegocioException.class,
                () -> h.encerrar(FormaPagamento.PIX, null, ENTRADA.plusDays(1))).getMessage());
        ItemConsumo caro = new ItemConsumo(2, "CARO", "Caro", "Serviço", new BigDecimal("99999999.99"));
        for (int i = 0; i < 101; i++) h.lancarConsumo(caro, Consumo.QUANTIDADE_MAXIMA, ENTRADA);
        assertThrows(NegocioException.class, () -> h.extrato(ENTRADA.plusDays(1)));
    }
}
