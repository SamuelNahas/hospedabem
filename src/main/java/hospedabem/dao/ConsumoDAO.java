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

    /** Grava o lançamento com o item e o preço aplicados; o total é calculado pelo banco (UC11). */
    public int inserir(int contaId, int itemId, Consumo c, int funcionarioId) {
        return jdbc.queryForObject("""
                INSERT INTO consumo (conta_hospedagem_id, item_consumo_id, codigo_item, descricao_item, quantidade,
                                     preco_unitario, data_hora, funcionario_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Integer.class,
                contaId, itemId, c.codigoItem(), c.descricaoItem(), c.quantidade(), c.precoUnitario(), c.dataHora(),
                funcionarioId);
    }

    public List<Consumo> listarPorConta(int contaId) {
        return jdbc.query("""
                SELECT id, codigo_item, descricao_item, quantidade, preco_unitario, data_hora
                FROM consumo WHERE conta_hospedagem_id = ? ORDER BY data_hora, id""",
                (rs, i) -> new Consumo(rs.getInt("id"), rs.getString("codigo_item"), rs.getString("descricao_item"),
                        rs.getInt("quantidade"), rs.getBigDecimal("preco_unitario"),
                        rs.getTimestamp("data_hora").toLocalDateTime()), contaId);
    }
}
