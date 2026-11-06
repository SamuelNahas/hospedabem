package hospedabem.controller;

import hospedabem.model.Consumo;
import hospedabem.model.Hospedagem;
import hospedabem.model.ItemConsumo;
import hospedabem.model.NegocioException;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** UC11 Lançar Consumo/Serviço, contra o PostgreSQL de teste. */
class ConsumoIT extends IntegracaoBase {
    @Autowired ControladorConsumo consumos;
    @Autowired ControladorItemConsumo itens;

    TipoQuarto std;
    Hospedagem h;
    ItemConsumo agua;

    @BeforeEach
    void prepararHospedagem() {
        std = tipos.listar().stream().filter(t -> t.codigo().equals("STD")).findFirst().orElseThrow();
        agua = itens.incluir(new ItemConsumo(0, "AGUA", "Água mineral", "Frigobar", new BigDecimal("5.50")));
        h = checkInSemReserva(std, 101, 2, CARLOS);
        logarComo("recep01");
    }


    @Test
    void recepcionistaLancaConsumoComPrecoTotalEDataPersistidos() {
        assertEquals(1, consumos.listarHospedagensAtivas().size());
        assertEquals(1, consumos.listarItens().size());
        Consumo c = consumos.lancar(h.getCodigo(), "agua", 3);
        assertTrue(c.id() > 0);
        assertEquals(new BigDecimal("16.50"), c.valorTotal());
        Hospedagem conta = consumos.localizar(h.getCodigo());
        assertEquals(1, conta.getConta().getConsumos().size());
        Consumo gravado = conta.getConta().getConsumos().get(0);
        assertEquals(3, gravado.quantidade());
        assertEquals(new BigDecimal("5.50"), gravado.precoUnitario());
        assertEquals(new BigDecimal("16.50"), gravado.valorTotal());
        assertNotNull(gravado.dataHora());
        assertEquals(1, contar("SELECT COUNT(*) FROM consumo WHERE valor_total = 16.50 AND funcionario_id = ?",
                idFuncionario("recep01")));
    }

    @Test
    void quantidadeInvalidaItemOuHospedagemInexistenteNaoGravam() {
        for (int q : new int[]{0, -2, 1000})
            assertThrows(NegocioException.class, () -> consumos.lancar(h.getCodigo(), "AGUA", q));
        assertThrows(NegocioException.class, () -> consumos.lancar(h.getCodigo(), "NAOEXISTE", 1));
        assertThrows(NegocioException.class, () -> consumos.lancar(99999, "AGUA", 1));
        assertThrows(NegocioException.class, () -> consumos.localizar(99999));
        assertEquals(0, contar("SELECT COUNT(*) FROM consumo"));
    }

    @Test
    void precoHistoricoEhPreservadoQuandoOCatalogoMuda() {
        consumos.lancar(h.getCodigo(), "AGUA", 2);
        logarComo("admin01");
        itens.alterar(new ItemConsumo(agua.id(), "AGUA", "Água mineral", "Frigobar", new BigDecimal("9.99")));
        consumos.lancar(h.getCodigo(), "AGUA", 1);
        itens.alterar(new ItemConsumo(agua.id(), "AGUA", "Água com gás", "Frigobar", new BigDecimal("9.99")));
        var lancados = consumos.localizar(h.getCodigo()).getConta().getConsumos();
        assertEquals(new BigDecimal("11.00"), lancados.get(0).valorTotal());
        assertEquals("Água mineral", lancados.get(1).descricaoItem());
        assertEquals(new BigDecimal("9.99"), lancados.get(1).valorTotal());
        assertEquals(new BigDecimal("20.99"), Consumo.somar(lancados));
        assertEquals(2, contar("SELECT COUNT(*) FROM consumo WHERE valor_total = preco_unitario * quantidade"));
    }

    @Test
    void reinicioDeOutraEstacaoNaoTravaATabelaDeConsumo() throws Exception {
        try (Connection leitora = jdbc.getDataSource().getConnection()) {
            leitora.setAutoCommit(false);
            leitora.createStatement().execute("LOCK TABLE consumo IN ACCESS SHARE MODE");
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> inicializador.inicializarSeVazio());
            leitora.rollback();
        }
    }

    @Test
    void maiorLancamentoAceitoCabeNoBancoETotalNaoPodeSerGravadoDivergente() {
        logarComo("admin01");
        itens.incluir(new ItemConsumo(0, "CARO", "Item caro", "Serviço", new BigDecimal("99999999.99")));
        logarComo("recep01");
        Consumo c = consumos.lancar(h.getCodigo(), "CARO", Consumo.QUANTIDADE_MAXIMA);
        assertEquals(new BigDecimal("99899999990.01"), c.valorTotal());
        assertEquals(c.valorTotal(), jdbc.queryForObject("SELECT valor_total FROM consumo", BigDecimal.class));
        assertThrows(RuntimeException.class, () -> jdbc.update("UPDATE consumo SET valor_total = 1"));
        assertThrows(RuntimeException.class, () -> jdbc.update("UPDATE consumo SET quantidade = 1000"));
    }

    @Test
    void lancamentoExigeSessaoDeRecepcionistaOuAdministrador() {
        sessao.encerrar();
        assertThrows(AcessoNegadoException.class, () -> consumos.lancar(h.getCodigo(), "AGUA", 1));
        assertThrows(AcessoNegadoException.class, () -> consumos.listarHospedagensAtivas());
        assertThrows(AcessoNegadoException.class, () -> consumos.localizar(h.getCodigo()));
        logarComo("admin01");
        consumos.lancar(h.getCodigo(), "AGUA", 1);
        assertEquals(1, contar("SELECT COUNT(*) FROM consumo"));
    }

    @Test
    void falhaDeGravacaoDoLancamentoNaoDeixaRegistroParcial() {
        jdbc.execute("CREATE FUNCTION falhar() RETURNS trigger AS $$ BEGIN RAISE EXCEPTION 'falha simulada'; END $$ LANGUAGE plpgsql");
        jdbc.execute("CREATE TRIGGER falha_consumo BEFORE INSERT ON consumo FOR EACH ROW EXECUTE FUNCTION falhar()");
        assertThrows(RuntimeException.class, () -> consumos.lancar(h.getCodigo(), "AGUA", 1));
        assertEquals(0, contar("SELECT COUNT(*) FROM consumo"));
    }
}
