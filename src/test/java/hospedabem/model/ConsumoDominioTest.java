package hospedabem.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Regras do UC11 no modelo, sem banco. */
class ConsumoDominioTest {
    static final LocalDateTime ENTRADA = LocalDateTime.of(2026, 11, 2, 14, 0);
    static final ItemConsumo AGUA = new ItemConsumo(1, "AGUA", "Água", "Frigobar", new BigDecimal("5.50"));

    static Hospedagem hospedagem() {
        TipoQuarto tipo = new TipoQuarto(1, "STD", "Standard", 2, new BigDecimal("180.00"));
        Quarto q = new Quarto(1, 101, 1, tipo, SituacaoQuarto.OCUPADO, true);
        return Hospedagem.reconstituir(1, 5001, ENTRADA, LocalDate.of(2026, 11, 5), SituacaoHospedagem.ATIVA, null, q,
                new ContaDaHospedagem(1, SituacaoConta.ABERTA, new BigDecimal("180.00")));
    }

    @Test
    void lancamentoGuardaPrecoETotalEValidaQuantidade() {
        Hospedagem h = hospedagem();
        Consumo c = h.lancarConsumo(AGUA, 3, ENTRADA.plusHours(1));
        assertEquals(new BigDecimal("5.50"), c.precoUnitario());
        assertEquals(new BigDecimal("16.50"), c.valorTotal());
        for (int invalida : new int[]{0, -1, Consumo.QUANTIDADE_MAXIMA + 1})
            assertThrows(NegocioException.class, () -> h.lancarConsumo(AGUA, invalida, ENTRADA));
        assertThrows(NegocioException.class, () -> h.lancarConsumo(null, 1, ENTRADA));
        assertEquals(1, h.getConta().getConsumos().size());
    }

    @Test
    void contaFechadaRecusaLancamento() {
        TipoQuarto tipo = new TipoQuarto(1, "STD", "Standard", 2, new BigDecimal("180.00"));
        Hospedagem h = Hospedagem.reconstituir(1, 5001, ENTRADA, LocalDate.of(2026, 11, 5), SituacaoHospedagem.ATIVA,
                null, new Quarto(1, 101, 1, tipo, SituacaoQuarto.OCUPADO, true),
                new ContaDaHospedagem(1, SituacaoConta.FECHADA, new BigDecimal("180.00")));
        assertThrows(NegocioException.class, () -> h.lancarConsumo(AGUA, 1, ENTRADA));
    }
}
