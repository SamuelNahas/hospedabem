package hospedabem.controller;

import hospedabem.App;
import hospedabem.dao.InicializadorBanco;
import hospedabem.dao.ReservaDAO;
import hospedabem.model.FormaPagamento;
import hospedabem.model.Hospedagem;
import hospedabem.model.ItemConsumo;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reiniciar a aplicação (novo contexto Spring, novas conexões) preserva os dados gravados. */
class PersistenciaIT extends IntegracaoBase {
    @Autowired ControladorItemConsumo itens;
    @Autowired ControladorConsumo consumos;
    @Autowired ControladorCheckOut checkOuts;

    @Test
    void dadosSobrevivemAoReinicio() {
        int codigo = reservas.registrar(DIEGO, "SUI", HOJE.plusDays(1), HOJE.plusDays(2), 1).getCodigo();
        TipoQuarto std = tipos.buscar("STD").orElseThrow();
        Hospedagem h = checkInSemReserva(std, 102, 1, CARLOS);

        try (ConfigurableApplicationContext reiniciado = new SpringApplicationBuilder(App.class)
                .profiles("test").headless(true).run()) {
            assertFalse(reiniciado.getBean(InicializadorBanco.class).inicializarSeVazio()); // não recria nem apaga
            assertTrue(reiniciado.getBean(ReservaDAO.class).buscarPorCodigo(codigo).isPresent());
            JdbcTemplate outro = reiniciado.getBean(JdbcTemplate.class);
            assertEquals("ABERTA", outro.queryForObject("SELECT c.situacao FROM conta_hospedagem c "
                    + "JOIN hospedagem h ON h.id = c.hospedagem_id WHERE h.codigo = ?", String.class, h.getCodigo()));
            assertEquals("OCUPADO", outro.queryForObject("SELECT situacao FROM quarto WHERE numero = 102", String.class));
        }
    }

    /** Iteração 2: consumo, pagamento, conta fechada, hospedagem encerrada e quarto liberado continuam após o reinício. */
    @Test
    void checkOutSobreviveAoReinicio() {
        itens.incluir(new ItemConsumo(0, "AGUA", "Água mineral", "Frigobar", new BigDecimal("5.50")));
        Hospedagem h = checkInSemReserva(tipos.buscar("STD").orElseThrow(), 102, 2, CARLOS);
        consumos.lancar(h.getCodigo(), "AGUA", 2);
        checkOuts.confirmarPagamento(h.getCodigo(), FormaPagamento.DINHEIRO, checkOuts.conferir(h.getCodigo()).total());

        try (ConfigurableApplicationContext reiniciado = new SpringApplicationBuilder(App.class)
                .profiles("test").headless(true).run()) {
            assertFalse(reiniciado.getBean(InicializadorBanco.class).inicializarSeVazio());
            Map<String, Object> estado = reiniciado.getBean(JdbcTemplate.class).queryForMap("""
                    SELECT h.situacao AS hospedagem, h.data_hora_saida IS NOT NULL AS saiu, c.situacao AS conta,
                           q.situacao AS quarto, p.forma, p.valor_total,
                           (SELECT SUM(valor_total) FROM consumo WHERE conta_hospedagem_id = c.id) AS consumos
                    FROM hospedagem h JOIN conta_hospedagem c ON c.hospedagem_id = h.id
                    JOIN quarto q ON q.id = h.quarto_id JOIN pagamento p ON p.conta_hospedagem_id = c.id
                    WHERE h.codigo = ?""", h.getCodigo());
            assertEquals("ENCERRADA", estado.get("hospedagem"));
            assertEquals(true, estado.get("saiu"));
            assertEquals("FECHADA", estado.get("conta"));
            assertEquals("DISPONIVEL", estado.get("quarto"));
            assertEquals("DINHEIRO", estado.get("forma"));
            assertEquals(new BigDecimal("191.00"), estado.get("valor_total")); // 1 diária STD + 2 × 5,50
            assertEquals(new BigDecimal("11.00"), estado.get("consumos"));
        }
    }
}
