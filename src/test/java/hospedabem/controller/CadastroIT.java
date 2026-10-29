package hospedabem.controller;

import hospedabem.model.NegocioException;
import hospedabem.model.Quarto;
import hospedabem.model.SituacaoQuarto;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC03 Gerenciar Tipo de Quarto e UC04 Gerenciar Quarto (RN-Q01 a RN-Q06). */
class CadastroIT extends IntegracaoBase {

    @Test
    void tipoValidoEhIncluidoBuscadoAlteradoEExcluido() {
        TipoQuarto t = criarTipo("fam", 5);
        assertEquals("FAM", tipos.buscar("FAM").orElseThrow().codigo());
        tipos.alterar(new TipoQuarto(t.id(), "FAM", "Família", 6, new BigDecimal("400.50")));
        assertEquals(new BigDecimal("400.50"), tipos.buscar("FAM").orElseThrow().valorDiaria());
        tipos.excluir(t);
        assertTrue(tipos.buscar("FAM").isEmpty());
    }

    @Test
    void tipoComCodigoDuplicadoOuValoresInvalidosEhRecusado() {
        criarTipo("DUP", 2);
        assertThrows(NegocioException.class, () -> criarTipo("dup", 3));
        assertThrows(NegocioException.class, () -> new TipoQuarto(0, "X", "X", 0, BigDecimal.TEN));
        assertThrows(NegocioException.class, () -> new TipoQuarto(0, "X", "X", 2, BigDecimal.ZERO));
        assertThrows(NegocioException.class, () -> new TipoQuarto(0, " ", "X", 2, BigDecimal.TEN));
    }

    @Test
    void tipoComQuartoOuReservaNaoPodeSerExcluido() {
        TipoQuarto t = criarTipo("A", 2);
        criarQuarto(901, t);
        assertThrows(NegocioException.class, () -> tipos.excluir(t));
        TipoQuarto lux = tipos.buscar("LUX").orElseThrow(); // possui a reserva 1001 da carga
        jdbc.update("DELETE FROM quarto WHERE tipo_quarto_id = ?", lux.id());
        assertThrows(NegocioException.class, () -> tipos.excluir(lux));
        assertTrue(tipos.buscar("A").isPresent());
    }

    @Test
    void capacidadeNaoPodeFicarAbaixoDeReservaExistente() {
        TipoQuarto lux = tipos.buscar("LUX").orElseThrow(); // reserva 1001 com 2 hóspedes
        assertThrows(NegocioException.class,
                () -> tipos.alterar(new TipoQuarto(lux.id(), "LUX", "Luxo", 1, lux.valorDiaria())));
        assertEquals(3, tipos.buscar("LUX").orElseThrow().capacidadeMaxima());
    }

    @Test
    void quartoValidoNumeroDuplicadoETipoInexistente() {
        TipoQuarto t = criarTipo("B", 2);
        criarQuarto(902, t);
        assertThrows(NegocioException.class, () -> criarQuarto(902, t));
        TipoQuarto fantasma = new TipoQuarto(99999, "NAOEXISTE", "x", 1, BigDecimal.ONE);
        assertThrows(NegocioException.class, () -> criarQuarto(903, fantasma));
        assertEquals(1, contar("SELECT COUNT(*) FROM quarto WHERE numero = 902"));
    }

    @Test
    void cadastroNaoDefineOcupadoNemReservado() {
        TipoQuarto t = criarTipo("C", 2);
        assertThrows(NegocioException.class, () -> quartos.incluir(new Quarto(0, 904, 9, t, SituacaoQuarto.OCUPADO)));
        Quarto q = criarQuarto(905, t);
        assertThrows(NegocioException.class,
                () -> quartos.alterar(new Quarto(q.getId(), 905, 9, t, SituacaoQuarto.RESERVADO)));
        assertEquals("DISPONIVEL", situacaoQuarto(905));
    }

    @Test
    void quartoComHospedagemAtivaNaoEhLiberadoReclassificadoManutencaoNemExcluido() {
        TipoQuarto t = criarTipo("D", 2);
        TipoQuarto outro = criarTipo("E", 2);
        Quarto q = criarQuarto(906, t);
        checkInSemReserva(t, 906, 2, CARLOS);
        assertThrows(NegocioException.class,
                () -> quartos.alterar(new Quarto(q.getId(), 906, 9, t, SituacaoQuarto.DISPONIVEL)));
        assertThrows(NegocioException.class,
                () -> quartos.alterar(new Quarto(q.getId(), 906, 9, t, SituacaoQuarto.MANUTENCAO)));
        assertThrows(NegocioException.class,
                () -> quartos.alterar(new Quarto(q.getId(), 906, 9, outro, SituacaoQuarto.OCUPADO)));
        assertThrows(NegocioException.class, () -> quartos.excluir(q));
        assertEquals("OCUPADO", situacaoQuarto(906));
    }

    @Test
    void quartoComHistoricoDeHospedagemNaoPodeSerExcluido() {
        TipoQuarto t = criarTipo("F", 2);
        Quarto q = criarQuarto(907, t);
        checkInSemReserva(t, 907, 1, CARLOS);
        jdbc.update("UPDATE hospedagem SET situacao = 'ENCERRADA', data_hora_saida = now()"); // simula check-out futuro
        assertThrows(NegocioException.class, () -> quartos.excluir(q));
        assertEquals(1, contar("SELECT COUNT(*) FROM quarto WHERE numero = 907"));
    }

    @Test
    void manutencaoTrocaDeTipoOuExclusaoQueInviabilizamReservaSaoRecusadasPreservandoDados() {
        TipoQuarto t = criarTipo("G", 2);
        TipoQuarto outro = criarTipo("H", 2);
        Quarto q = criarQuarto(908, t);
        reservas.registrar(CARLOS, "G", HOJE.plusDays(5), HOJE.plusDays(7), 1);

        assertThrows(NegocioException.class,
                () -> quartos.alterar(new Quarto(q.getId(), 908, 9, t, SituacaoQuarto.MANUTENCAO)));
        assertThrows(NegocioException.class,
                () -> quartos.alterar(new Quarto(q.getId(), 908, 9, outro, SituacaoQuarto.DISPONIVEL)));
        assertThrows(NegocioException.class, () -> quartos.excluir(q));

        Quarto atual = quartos.listar().stream().filter(x -> x.getNumero() == 908).findFirst().orElseThrow();
        assertEquals(SituacaoQuarto.DISPONIVEL, atual.getSituacao());
        assertEquals("G", atual.getTipo().codigo());
    }

    @Test
    void manutencaoPermitidaQuandoSobraQuartoParaAsReservas() {
        TipoQuarto t = criarTipo("I", 2);
        Quarto q = criarQuarto(909, t);
        criarQuarto(910, t);
        reservas.registrar(CARLOS, "I", HOJE.plusDays(5), HOJE.plusDays(7), 1);
        quartos.alterar(new Quarto(q.getId(), 909, 9, t, SituacaoQuarto.MANUTENCAO));
        assertEquals("MANUTENCAO", situacaoQuarto(909));
    }
}
