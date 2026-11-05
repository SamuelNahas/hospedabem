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

    int idFuncionario(String matricula) {
        return funcionarioDAO.buscarCredencial(matricula).orElseThrow().funcionario().id();
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
        var lancados = consumos.localizar(h.getCodigo()).getConta().getConsumos();
        assertEquals(new BigDecimal("11.00"), lancados.get(0).valorTotal());
        assertEquals(new BigDecimal("9.99"), lancados.get(1).valorTotal());
        assertEquals(new BigDecimal("20.99"), lancados.stream().map(Consumo::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
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
