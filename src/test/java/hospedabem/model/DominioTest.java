package hospedabem.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DominioTest {
    static final LocalDate D = LocalDate.of(2026, 11, 10);
    static final TipoQuarto LUXO = new TipoQuarto(1, "lux ", " Luxo ", 3, new BigDecimal("320.00"));
    static final Hospede ANA = new Hospede(7, "111", "Ana", D.minusYears(30), null, null, null, "67", null);

    static Reserva reserva(int versao, SituacaoReserva situacao) {
        return new Reserva(5, 1001, ANA, LUXO, new Periodo(D, D.plusDays(2)), 2, situacao, 1, versao);
    }

    @Test
    void tipoDeQuartoNormalizaCodigoEDescricao() {
        assertEquals("LUX", LUXO.codigo());
        assertEquals("Luxo", LUXO.descricao());
        assertEquals("LUX - Luxo", LUXO.toString());
    }

    @Test
    void tipoDeQuartoExigeCamposECapacidadeEDiariaPositivas() {
        assertThrows(NegocioException.class, () -> new TipoQuarto(0, null, "x", 1, BigDecimal.ONE));
        assertThrows(NegocioException.class, () -> new TipoQuarto(0, "A", " ", 1, BigDecimal.ONE));
        assertThrows(NegocioException.class, () -> new TipoQuarto(0, "A", "x", 0, BigDecimal.ONE));
        assertThrows(NegocioException.class, () -> new TipoQuarto(0, "A", "x", 1, null));
        assertThrows(NegocioException.class, () -> new TipoQuarto(0, "A", "x", 1, new BigDecimal("-0.01")));
        assertEquals(1, new TipoQuarto(0, "A", "x", 1, new BigDecimal("0.01")).capacidadeMaxima());
    }

    @Test
    void quartoExigeNumeroPositivoTipoESituacaoEOcupar() {
        assertThrows(NegocioException.class, () -> new Quarto(0, 0, 1, LUXO, SituacaoQuarto.DISPONIVEL));
        assertThrows(NegocioException.class, () -> new Quarto(0, 1, 1, null, SituacaoQuarto.DISPONIVEL));
        assertThrows(NegocioException.class, () -> new Quarto(0, 1, 1, LUXO, null));
        Quarto q = new Quarto(3, 1, 2, LUXO, SituacaoQuarto.DISPONIVEL);
        assertEquals(2, q.getAndar());
        q.ocupar();
        assertEquals(SituacaoQuarto.OCUPADO, q.getSituacao());
        assertEquals("Quarto 1", q.toString());
    }

    @Test
    void hospedagemExigeSaidaPrevistaPosteriorAoDiaDeEntrada() {
        LocalDateTime entrada = D.atTime(23, 59);
        assertThrows(NegocioException.class, () -> new Hospedagem(entrada, D));
        assertThrows(NegocioException.class, () -> new Hospedagem(entrada, D.minusDays(1)));
        assertEquals(D.plusDays(1), new Hospedagem(entrada, D.plusDays(1)).getDataSaidaPrevista());
    }

    @Test
    void hospedagemAbreContaComADiariaDoTipoAoReceberOQuarto() {
        Hospedagem h = new Hospedagem(D.atStartOfDay(), D.plusDays(1));
        Quarto q = new Quarto(3, 201, 2, LUXO, SituacaoQuarto.DISPONIVEL);
        h.setQuarto(q);
        h.adicionarHospede(ANA);
        assertSame(q, h.getQuarto());
        assertEquals(SituacaoConta.ABERTA, h.getConta().getSituacao());
        assertEquals(new BigDecimal("320.00"), h.getConta().getValorDiaria());
        assertEquals(SituacaoHospedagem.ATIVA, h.getSituacao());
        assertEquals(1, h.getHospedes().size());
        assertThrows(UnsupportedOperationException.class, () -> h.getHospedes().add(ANA));
    }

    @Test
    void converterReservaMarcaConvertidaEVinculaAHospedagem() {
        Reserva r = reserva(0, SituacaoReserva.CONFIRMADA);
        Hospedagem h = new Hospedagem(D.atStartOfDay(), D.plusDays(2));
        r.converter(h);
        assertEquals(SituacaoReserva.CONVERTIDA, r.getSituacao());
        assertSame(r, h.getReserva());
    }

    @Test
    void mesmoEstadoDetectaQualquerAlteracaoRelevante() {
        Reserva base = reserva(0, SituacaoReserva.CONFIRMADA);
        assertTrue(base.mesmoEstadoDe(reserva(0, SituacaoReserva.CONFIRMADA)));
        assertEquals(2, base.getQuantidadeHospedes());
        assertFalse(base.mesmoEstadoDe(null));
        assertFalse(base.mesmoEstadoDe(reserva(1, SituacaoReserva.CONFIRMADA)));
        assertFalse(base.mesmoEstadoDe(reserva(0, SituacaoReserva.CANCELADA)));
        assertFalse(base.mesmoEstadoDe(new Reserva(6, 1001, ANA, LUXO, base.getPeriodo(), 2, SituacaoReserva.CONFIRMADA, 1, 0)));
        assertFalse(base.mesmoEstadoDe(new Reserva(5, 1001, ANA, LUXO, new Periodo(D, D.plusDays(3)), 2,
                SituacaoReserva.CONFIRMADA, 1, 0)));
        assertFalse(base.mesmoEstadoDe(new Reserva(5, 1001, ANA, LUXO, base.getPeriodo(), 3, SituacaoReserva.CONFIRMADA, 1, 0)));
        TipoQuarto outroTipo = new TipoQuarto(2, "STD", "Standard", 3, BigDecimal.TEN);
        assertFalse(base.mesmoEstadoDe(new Reserva(5, 1001, ANA, outroTipo, base.getPeriodo(), 2,
                SituacaoReserva.CONFIRMADA, 1, 0)));
        Hospede outro = new Hospede(8, "222", "Bia", D, null, null, null, "67", null);
        assertFalse(base.mesmoEstadoDe(new Reserva(5, 1001, outro, LUXO, base.getPeriodo(), 2,
                SituacaoReserva.CONFIRMADA, 1, 0)));
    }

    @Test
    void capacidadeAceitaOLimiteERecusaAlem() {
        LUXO.exigirComporta(3);
        assertEquals("O tipo LUX comporta 3 hóspede(s); são necessários 4.",
                assertThrows(NegocioException.class, () -> LUXO.exigirComporta(4)).getMessage());
    }

    @Test
    void cadastroSoDefineDisponivelOuManutencao() {
        assertTrue(SituacaoQuarto.DISPONIVEL.definivelNoCadastro());
        assertTrue(SituacaoQuarto.MANUTENCAO.definivelNoCadastro());
        assertFalse(SituacaoQuarto.OCUPADO.definivelNoCadastro());
        assertFalse(SituacaoQuarto.RESERVADO.definivelNoCadastro());
    }

    @Test
    void quartoComHospedagemAtivaSoMudaNumeroOuAndar() {
        Quarto livre = new Quarto(3, 201, 2, LUXO, SituacaoQuarto.DISPONIVEL);
        livre.exigirAlteracaoPara(new Quarto(3, 201, 2, LUXO, SituacaoQuarto.MANUTENCAO));
        assertThrows(NegocioException.class, () -> livre.exigirAlteracaoPara(new Quarto(3, 201, 2, LUXO, SituacaoQuarto.OCUPADO)));

        Quarto hospedado = new Quarto(3, 201, 2, LUXO, SituacaoQuarto.OCUPADO, true);
        hospedado.exigirAlteracaoPara(new Quarto(3, 205, 3, LUXO, SituacaoQuarto.OCUPADO));
        TipoQuarto outro = new TipoQuarto(2, "STD", "Standard", 2, BigDecimal.TEN);
        assertThrows(NegocioException.class, () -> hospedado.exigirAlteracaoPara(new Quarto(3, 201, 2, outro, SituacaoQuarto.OCUPADO)));
        assertThrows(NegocioException.class, () -> hospedado.exigirAlteracaoPara(new Quarto(3, 201, 2, LUXO, SituacaoQuarto.DISPONIVEL)));
    }

    @Test
    void quartoLivreParaEntradaEhDoTipoSemHospedagemENemEmManutencao() {
        new Quarto(3, 201, 2, LUXO, SituacaoQuarto.DISPONIVEL).exigirLivrePara(LUXO);
        TipoQuarto outro = new TipoQuarto(2, "STD", "Standard", 2, BigDecimal.TEN);
        assertThrows(NegocioException.class, () -> new Quarto(3, 201, 2, LUXO, SituacaoQuarto.DISPONIVEL).exigirLivrePara(outro));
        assertThrows(NegocioException.class, () -> new Quarto(3, 201, 2, LUXO, SituacaoQuarto.MANUTENCAO).exigirLivrePara(LUXO));
        assertThrows(NegocioException.class, () -> new Quarto(3, 201, 2, LUXO, SituacaoQuarto.OCUPADO, true).exigirLivrePara(LUXO));
    }

    @Test
    void reservaPermiteCheckInDaEntradaAteAVesperaDaSaida() {
        Reserva r = reserva(0, SituacaoReserva.CONFIRMADA); // D a D+2
        assertThrows(NegocioException.class, () -> r.exigirPermiteCheckIn(D.minusDays(1)));
        r.exigirPermiteCheckIn(D);
        r.exigirPermiteCheckIn(D.plusDays(1));
        assertThrows(NegocioException.class, () -> r.exigirPermiteCheckIn(D.plusDays(2)));
        assertThrows(NegocioException.class, () -> reserva(0, SituacaoReserva.CANCELADA).exigirPermiteCheckIn(D));
    }

    @Test
    void solicitacaoDeReservaValidaDataQuantidadeECapacidade() {
        Periodo dois = new Periodo(D, D.plusDays(2));
        new SolicitacaoReserva(ANA, LUXO, dois, 3).validar(D);
        assertThrows(NegocioException.class, () -> new SolicitacaoReserva(ANA, LUXO, dois, 1).validar(D.plusDays(1)));
        assertThrows(NegocioException.class, () -> new SolicitacaoReserva(ANA, LUXO, dois, 0).validar(D));
        assertThrows(NegocioException.class, () -> new SolicitacaoReserva(ANA, LUXO, dois, 4).validar(D));
    }

    @Test
    void disponibilidadeExigeAoMenosUmQuartoLivre() {
        new Disponibilidade(LUXO, 2, 1).exigirVaga();
        assertEquals("Não há quarto do tipo LUX disponível no período.",
                assertThrows(NegocioException.class, () -> new Disponibilidade(LUXO, 2, 2).exigirVaga()).getMessage());
        assertEquals(0, new Disponibilidade(LUXO, 1, 3).livres());
    }
}
