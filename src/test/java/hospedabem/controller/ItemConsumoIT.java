package hospedabem.controller;

import hospedabem.App;
import hospedabem.dao.InicializadorBanco;
import hospedabem.model.ItemConsumo;
import hospedabem.model.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ItemConsumoIT extends IntegracaoBase {
    @Autowired ControladorItemConsumo itens;

    @Test
    void administradorIncluiBuscaListaAlteraEExcluiItem() {
        ItemConsumo item = itens.incluir(new ItemConsumo(0, " agua ", " Água mineral ", " Frigobar ", new BigDecimal("5.50")));
        assertTrue(item.id() > 0);
        assertEquals(item, itens.buscar(" agua ").orElseThrow());
        assertEquals("AGUA", item.codigo());
        assertEquals("Água mineral", item.descricao());
        assertEquals("Frigobar", item.categoria());
        assertEquals(java.util.List.of(item), itens.listar());

        itens.alterar(new ItemConsumo(item.id(), "AGUA2", "Água com gás", "Restaurante", new BigDecimal("6.00")));
        assertTrue(itens.buscar("AGUA").isEmpty());
        ItemConsumo alterado = itens.buscar("AGUA2").orElseThrow();
        assertEquals("Água com gás", alterado.descricao());
        assertEquals("Restaurante", alterado.categoria());
        assertEquals(new BigDecimal("6.00"), alterado.preco());
        itens.excluir(alterado);
        assertTrue(itens.listar().isEmpty());
    }

    /** Exercita campos ausentes, tamanhos excedidos e preços inválidos sem permitir gravações. */
    @Test
    void camposObrigatoriosLimitesEPrecisaoMonetariaSaoValidadosAntesDeGravar() {
        for (String invalido : new String[]{null, "", " ", "X".repeat(121)}) {
            assertThrows(NegocioException.class,
                    () -> itens.incluir(new ItemConsumo(0, invalido, "Água", "Frigobar", BigDecimal.ONE)));
            assertThrows(NegocioException.class,
                    () -> itens.incluir(new ItemConsumo(0, "AGUA", invalido, "Frigobar", BigDecimal.ONE)));
            assertThrows(NegocioException.class,
                    () -> itens.incluir(new ItemConsumo(0, "AGUA", "Água", invalido, BigDecimal.ONE)));
        }
        assertThrows(NegocioException.class,
                () -> itens.incluir(new ItemConsumo(0, "X".repeat(21), "Água", "Frigobar", BigDecimal.ONE)));
        assertThrows(NegocioException.class,
                () -> itens.incluir(new ItemConsumo(0, "AGUA", "Água", "X".repeat(81), BigDecimal.ONE)));
        for (BigDecimal invalido : java.util.Arrays.asList(null, BigDecimal.ZERO, new BigDecimal("-0.01"),
                new BigDecimal("1.001"), new BigDecimal("100000000.00"))) {
            assertThrows(NegocioException.class,
                    () -> itens.incluir(new ItemConsumo(0, "AGUA", "Água", "Frigobar", invalido)));
        }
        assertTrue(itens.listar().isEmpty());
    }

    @Test
    void duplicidadeNaInclusaoEAlteracaoPreservaOsItens() {
        ItemConsumo agua = itens.incluir(new ItemConsumo(0, "AGUA", "Água", "Frigobar", BigDecimal.ONE));
        ItemConsumo cafe = itens.incluir(new ItemConsumo(0, "CAFE", "Café", "Restaurante", BigDecimal.TEN));
        assertThrows(NegocioException.class,
                () -> itens.incluir(new ItemConsumo(0, " agua ", "Duplicado", "Lavanderia", BigDecimal.TEN)));
        assertThrows(NegocioException.class,
                () -> itens.alterar(new ItemConsumo(cafe.id(), "agua", "Duplicado", "Lavanderia", BigDecimal.ONE)));
        assertEquals(agua, itens.buscar("AGUA").orElseThrow());
        assertEquals(cafe, itens.buscar("CAFE").orElseThrow());
        assertEquals(2, itens.listar().size());
    }

    @Test
    void alteracaoEExclusaoDeItemInexistenteSaoRecusadas() {
        ItemConsumo inexistente = new ItemConsumo(99999, "NAOEXISTE", "Inexistente", "Frigobar", BigDecimal.ONE);
        assertTrue(itens.buscar("NAOEXISTE").isEmpty());
        assertTrue(itens.buscar(null).isEmpty());
        assertThrows(NegocioException.class, () -> itens.alterar(inexistente));
        assertThrows(NegocioException.class, () -> itens.excluir(inexistente));
    }

    /** Confere todas as operações primeiro como recepcionista e depois sem sessão autenticada. */
    @Test
    void recepcionistaESessaoAusenteNaoPodemExecutarNenhumaOperacao() {
        ItemConsumo item = itens.incluir(new ItemConsumo(0, "AGUA", "Água", "Frigobar", BigDecimal.ONE));
        logarComo("recep01");
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            assertThrows(AcessoNegadoException.class, itens::listar);
            assertThrows(AcessoNegadoException.class, () -> itens.buscar("AGUA"));
            assertThrows(AcessoNegadoException.class, () -> itens.incluir(item));
            assertThrows(AcessoNegadoException.class, () -> itens.alterar(item));
            assertThrows(AcessoNegadoException.class, () -> itens.excluir(item));
            sessao.encerrar();
        }
        logarComo("admin01");
        assertEquals(java.util.List.of(item), itens.listar());
    }

    @Test
    void aceitaLimitesDeCamposECentavosSemArredondar() {
        ItemConsumo minimo = itens.incluir(new ItemConsumo(0, "X".repeat(20), "D".repeat(120),
                "C".repeat(80), new BigDecimal("0.01")));
        assertEquals(minimo, itens.buscar(minimo.codigo()).orElseThrow());
        itens.alterar(new ItemConsumo(minimo.id(), minimo.codigo(), minimo.descricao(), minimo.categoria(),
                new BigDecimal("99999999.990")));
        assertEquals(new BigDecimal("99999999.99"), itens.buscar(minimo.codigo()).orElseThrow().preco());
    }

    @Test
    void limiteDoCodigoConsideraExpansaoParaMaiusculas() {
        NegocioException erro = assertThrows(NegocioException.class,
                () -> itens.incluir(new ItemConsumo(0, "ß".repeat(11), "Item", "Serviço", BigDecimal.ONE)));
        assertEquals("Código deve ter no máximo 20 caracteres.", erro.getMessage());
        assertTrue(itens.listar().isEmpty());
    }

    @Test
    void itemUtilizadoNaoPodeSerExcluidoEAlterarCatalogoNaoRecalculaConsumo() {
        ItemConsumo item = itens.incluir(new ItemConsumo(0, "AGUA", "Água", "Frigobar", new BigDecimal("5.50")));
        var hospedagem = checkInSemReserva(tipos.buscar("STD").orElseThrow(), 101, 2, CARLOS);
        // Fixture do UC11: neste UC05 ainda não existe operação de lançamento pela interface.
        int consumo = jdbc.queryForObject("""
                INSERT INTO consumo (conta_hospedagem_id, item_consumo_id, quantidade, preco_unitario, valor_total)
                VALUES (?, ?, 2, 5.50, 11.00) RETURNING id""", Integer.class, hospedagem.getConta().getId(), item.id());
        itens.alterar(new ItemConsumo(item.id(), "AGUA", "Água", "Frigobar", new BigDecimal("9.00")));
        assertEquals(new BigDecimal("9.00"), itens.buscar("AGUA").orElseThrow().preco());
        assertEquals(new BigDecimal("5.50"), jdbc.queryForObject(
                "SELECT preco_unitario FROM consumo WHERE id = ?", BigDecimal.class, consumo));
        assertThrows(NegocioException.class, () -> itens.excluir(item));
        assertTrue(itens.buscar("AGUA").isPresent());
        assertEquals(1, contar("SELECT COUNT(*) FROM consumo WHERE id = ?", consumo));
    }

    /** Atualiza um banco da iteração 1 e confere o catálogo e a reserva após reiniciar a aplicação. */
    @Test
    void migracaoAtualizaBancoDaIteracaoUmSemPerderDadosEReinicioPreservaCatalogo() {
        int reserva = reservas.registrar(DIEGO, "SUI", HOJE.plusDays(3), HOJE.plusDays(5), 1).getCodigo();
        jdbc.execute("DROP TABLE consumo");
        jdbc.execute("DROP TABLE item_consumo");
        assertFalse(inicializador.inicializarSeVazio());
        assertTrue(itens.listar().isEmpty());
        ItemConsumo item = itens.incluir(new ItemConsumo(0, "LAVAR", "Lavar roupa", "Lavanderia", BigDecimal.TEN));
        try (ConfigurableApplicationContext reiniciado = new SpringApplicationBuilder(App.class)
                .profiles("test").headless(true).run()) {
            assertFalse(reiniciado.getBean(InicializadorBanco.class).inicializarSeVazio());
            reiniciado.getBean(ControladorAutenticacao.class).autenticar("admin01", "Admin@123".toCharArray());
            assertEquals(item, reiniciado.getBean(ControladorItemConsumo.class).buscar("LAVAR").orElseThrow());
            assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE codigo = ?", reserva));
        }
    }
}
