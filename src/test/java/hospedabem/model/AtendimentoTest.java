package hospedabem.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtendimentoTest {
    static final LocalDate HOJE = LocalDate.of(2026, 11, 10);
    static final TipoQuarto DUPLO = new TipoQuarto(1, "STD", "Standard", 2, new BigDecimal("180.00"));
    static final Hospede ANA = hospede(1, "Ana");
    static final Hospede BIA = hospede(2, "Bia");
    static final Hospede CAIO = hospede(3, "Caio");

    static Hospede hospede(int id, String nome) {
        return new Hospede(id, "doc" + id, nome, HOJE.minusYears(30), null, null, null, "67", null);
    }

    static Reserva reserva(SituacaoReserva situacao, int versao) {
        return new Reserva(9, 1001, ANA, DUPLO, new Periodo(HOJE, HOJE.plusDays(3)), 2, situacao, 1, versao);
    }

    static Quarto quarto(boolean hospedado) {
        return new Quarto(5, 101, 1, DUPLO, SituacaoQuarto.DISPONIVEL, hospedado);
    }

    static Atendimento semReserva() {
        return Atendimento.semReserva(ANA, DUPLO, new Periodo(HOJE, HOJE.plusDays(2)), HOJE);
    }

    @Test
    void semReservaExigeEntradaHoje() {
        assertThrows(NegocioException.class,
                () -> Atendimento.semReserva(ANA, DUPLO, new Periodo(HOJE.plusDays(1), HOJE.plusDays(2)), HOJE));
        assertNull(semReserva().getReserva());
    }

    @Test
    void comReservaVaiDeHojeAteASaidaDaReserva() {
        Atendimento a = Atendimento.comReserva(reserva(SituacaoReserva.CONFIRMADA, 0), HOJE.plusDays(1));
        assertEquals(new Periodo(HOJE.plusDays(1), HOJE.plusDays(3)), a.getPeriodo());
        assertSame(ANA, a.getResponsavel());
        assertThrows(NegocioException.class, () -> Atendimento.comReserva(reserva(SituacaoReserva.CONVERTIDA, 0), HOJE));
    }

    @Test
    void hospedeRepetidoOuAlemDaCapacidadeEhRecusado() {
        Atendimento a = semReserva();
        a.adicionar(ANA);
        assertEquals("Hóspede já informado: Ana.", assertThrows(NegocioException.class, () -> a.adicionar(ANA)).getMessage());
        a.adicionar(BIA);
        assertEquals("O tipo STD comporta 2 hóspede(s); são necessários 3.",
                assertThrows(NegocioException.class, () -> a.adicionar(CAIO)).getMessage());
        a.remover(BIA);
        assertEquals(1, a.getHospedes().size());
    }

    @Test
    void responsavelPrecisaParticipar() {
        Atendimento a = semReserva();
        assertEquals("Informe ao menos um hóspede.", assertThrows(NegocioException.class, a::exigirHospedes).getMessage());
        a.adicionar(BIA);
        assertThrows(NegocioException.class, a::exigirHospedes);
        a.adicionar(ANA);
        a.exigirHospedes();
    }

    @Test
    void quartoSelecionadoEhDescartadoQuandoOsHospedesMudam() {
        Atendimento a = semReserva();
        a.adicionar(ANA);
        assertThrows(NegocioException.class, () -> a.selecionar(quarto(true)));
        a.selecionar(quarto(false));
        a.exigirProntoParaConfirmar();
        a.adicionar(BIA);
        assertNull(a.getQuarto());
        assertEquals("Selecione o quarto antes de confirmar.",
                assertThrows(NegocioException.class, a::exigirProntoParaConfirmar).getMessage());
    }

    @Test
    void reservaAlteradaOuCanceladaAposOResumoEhDetectada() {
        Atendimento a = Atendimento.comReserva(reserva(SituacaoReserva.CONFIRMADA, 0), HOJE);
        a.exigirReservaInalterada(reserva(SituacaoReserva.CONFIRMADA, 0));
        assertThrows(NegocioException.class, () -> a.exigirReservaInalterada(reserva(SituacaoReserva.CONFIRMADA, 1)));
        assertThrows(NegocioException.class, () -> a.exigirReservaInalterada(reserva(SituacaoReserva.CANCELADA, 1)));
        semReserva().exigirReservaInalterada(null);
    }

    @Test
    void criarHospedagemCumpreOContratoDoCheckIn() {
        Reserva r = reserva(SituacaoReserva.CONFIRMADA, 0);
        Atendimento a = Atendimento.comReserva(r, HOJE);
        a.adicionar(ANA);
        a.adicionar(BIA);
        Quarto q = quarto(false);
        Hospedagem h = a.criarHospedagem(q, r, HOJE.atTime(14, 0));
        assertEquals(2, h.getHospedes().size());
        assertSame(q, h.getQuarto());
        assertEquals(SituacaoQuarto.OCUPADO, q.getSituacao());
        assertEquals(SituacaoConta.ABERTA, h.getConta().getSituacao());
        assertEquals(SituacaoReserva.CONVERTIDA, r.getSituacao());
        assertEquals(HOJE.plusDays(3), h.getDataSaidaPrevista());
    }

    @Test
    void criarHospedagemRevalidaQuartoECapacidadeAtuais() {
        Atendimento a = semReserva();
        a.adicionar(ANA);
        a.adicionar(BIA);
        assertThrows(NegocioException.class, () -> a.criarHospedagem(quarto(true), null, HOJE.atTime(9, 0)));
        TipoQuarto reduzido = new TipoQuarto(1, "STD", "Standard", 1, BigDecimal.TEN);
        Quarto q = new Quarto(5, 101, 1, reduzido, SituacaoQuarto.DISPONIVEL);
        assertThrows(NegocioException.class, () -> a.criarHospedagem(q, null, HOJE.atTime(9, 0)));
        assertEquals(SituacaoQuarto.DISPONIVEL, q.getSituacao());
    }
}
