package hospedabem.dao;

import hospedabem.model.ItemConsumo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class ItemConsumoDAO {
    private final JdbcTemplate jdbc;

    public ItemConsumoDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ItemConsumo> listar() {
        return jdbc.query("SELECT * FROM item_consumo ORDER BY codigo", (rs, i) ->
                new ItemConsumo(rs.getInt("id"), rs.getString("codigo"), rs.getString("descricao"),
                        rs.getString("categoria"), rs.getBigDecimal("preco")));
    }

    /** Busca pelo código normalizado; códigos nulos ou inexistentes não retornam item. */
    public Optional<ItemConsumo> buscarPorCodigo(String codigo) {
        return jdbc.query("SELECT * FROM item_consumo WHERE codigo = ?", (rs, i) ->
                new ItemConsumo(rs.getInt("id"), rs.getString("codigo"), rs.getString("descricao"),
                        rs.getString("categoria"), rs.getBigDecimal("preco")),
                codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT)).stream().findFirst();
    }

    public int inserir(ItemConsumo item) {
        return jdbc.queryForObject("INSERT INTO item_consumo (codigo, descricao, categoria, preco) VALUES (?, ?, ?, ?) RETURNING id",
                Integer.class, item.codigo(), item.descricao(), item.categoria(), item.preco());
    }

    public int atualizar(ItemConsumo item) {
        return jdbc.update("UPDATE item_consumo SET codigo = ?, descricao = ?, categoria = ?, preco = ? WHERE id = ?",
                item.codigo(), item.descricao(), item.categoria(), item.preco(), item.id());
    }

    public int excluir(int id) {
        return jdbc.update("DELETE FROM item_consumo WHERE id = ?", id);
    }
}
