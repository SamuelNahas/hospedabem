package hospedabem.controller;

import hospedabem.BancoDeTeste;
import hospedabem.dao.InicializadorBanco;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = "hb.demo=true")
@ActiveProfiles("test")
class DemonstracaoIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired InicializadorBanco inicializador;

    @Test
    void cargaDemonstraPagamentoPendenteEConcluidoSemRecarregarNoReinicio() {
        BancoDeTeste.recriar(jdbc, inicializador);
        assertEquals("CANCELADA", jdbc.queryForObject(
                "SELECT situacao FROM reserva WHERE codigo = 2005", String.class));
        List<Map<String, Object>> contas = jdbc.queryForList("""
                SELECT h.codigo, h.situacao AS hospedagem, c.situacao AS conta, q.situacao AS quarto,
                       SUM(co.valor_total) AS consumos, p.valor_total AS pagamento
                FROM hospedagem h JOIN conta_hospedagem c ON c.hospedagem_id = h.id
                JOIN quarto q ON q.id = h.quarto_id
                JOIN consumo co ON co.conta_hospedagem_id = c.id
                LEFT JOIN pagamento p ON p.conta_hospedagem_id = c.id
                GROUP BY h.codigo, h.situacao, c.situacao, q.situacao, p.valor_total ORDER BY h.codigo""");
        assertEquals(2, contas.size());
        assertEquals(9001, contas.get(0).get("codigo"));
        assertEquals("ATIVA", contas.get(0).get("hospedagem"));
        assertEquals("ABERTA", contas.get(0).get("conta"));
        assertEquals("OCUPADO", contas.get(0).get("quarto"));
        assertEquals(new BigDecimal("11.00"), contas.get(0).get("consumos"));
        assertNull(contas.get(0).get("pagamento"));
        assertEquals(9003, contas.get(1).get("codigo"));
        assertEquals("ENCERRADA", contas.get(1).get("hospedagem"));
        assertEquals("FECHADA", contas.get(1).get("conta"));
        assertEquals("DISPONIVEL", contas.get(1).get("quarto"));
        assertEquals(new BigDecimal("531.00"), contas.get(1).get("pagamento"));
        assertFalse(inicializador.inicializarSeVazio());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM hospedagem", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM consumo", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM pagamento", Integer.class));
    }
}
