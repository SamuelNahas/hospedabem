package hospedabem.dao;

import hospedabem.model.Consumo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ConsumoDAO {
    private final JdbcTemplate jdbc;

    public ConsumoDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Grava o lançamento com o preço aplicado e o total (UC11). */
    public int inserir(int contaId, int itemId, Consumo c, int funcionarioId) {
        return jdbc.queryForObject("""
                INSERT INTO consumo (conta_hospedagem_id, item_consumo_id, quantidade, preco_unitario, valor_total,
                                     data_hora, funcionario_id)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id""", Integer.class,
                contaId, itemId, c.quantidade(), c.precoUnitario(), c.valorTotal(), c.dataHora(), funcionarioId);
    }

    public List<Consumo> listarPorConta(int contaId) {
        return jdbc.query("""
                SELECT c.id, i.codigo, i.descricao, c.quantidade, c.preco_unitario, c.valor_total, c.data_hora
                FROM consumo c JOIN item_consumo i ON i.id = c.item_consumo_id
                WHERE c.conta_hospedagem_id = ? ORDER BY c.data_hora, c.id""",
                (rs, i) -> new Consumo(rs.getInt("id"), rs.getString("codigo"), rs.getString("descricao"),
                        rs.getInt("quantidade"), rs.getBigDecimal("preco_unitario"), rs.getBigDecimal("valor_total"),
                        rs.getTimestamp("data_hora").toLocalDateTime()), contaId);
    }
}
