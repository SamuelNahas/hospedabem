package hospedabem.controller;

import hospedabem.App;
import hospedabem.dao.InicializadorBanco;
import hospedabem.dao.ReservaDAO;
import hospedabem.model.Hospedagem;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reiniciar a aplicação (novo contexto Spring, novas conexões) preserva os dados gravados. */
class PersistenciaIT extends IntegracaoBase {

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
}
