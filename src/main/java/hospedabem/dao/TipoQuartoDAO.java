package hospedabem.dao;

import hospedabem.model.TipoQuarto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

@Repository
public class TipoQuartoDAO {
    private final JdbcTemplate jdbc;

    public TipoQuartoDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TipoQuarto> listar() {
        return jdbc.query("SELECT * FROM tipo_quarto ORDER BY codigo", (rs, i) -> ler(rs, ""));
    }

    public Optional<TipoQuarto> buscarPorCodigo(String codigo) {
        return jdbc.query("SELECT * FROM tipo_quarto WHERE codigo = ?", (rs, i) -> ler(rs, ""),
                codigo == null ? "" : codigo.trim().toUpperCase()).stream().findFirst();
    }

    // Ordem crescente de id evita deadlock entre transações que bloqueiam mais de um tipo.
    public void bloquear(Collection<Integer> ids) {
        for (int id : new TreeSet<>(ids)) jdbc.queryForList("SELECT id FROM tipo_quarto WHERE id = ? FOR UPDATE", id);
    }

    public int inserir(TipoQuarto t) {
        return jdbc.queryForObject("INSERT INTO tipo_quarto (codigo, descricao, capacidade_maxima, valor_diaria) VALUES (?, ?, ?, ?) RETURNING id",
                Integer.class, t.codigo(), t.descricao(), t.capacidadeMaxima(), t.valorDiaria());
    }

    public void atualizar(TipoQuarto t) {
        jdbc.update("UPDATE tipo_quarto SET codigo = ?, descricao = ?, capacidade_maxima = ?, valor_diaria = ? WHERE id = ?",
                t.codigo(), t.descricao(), t.capacidadeMaxima(), t.valorDiaria(), t.id());
    }

    public void excluir(int id) {
        jdbc.update("DELETE FROM tipo_quarto WHERE id = ?", id);
    }

    public int maiorOcupacaoVigente(int tipoId, LocalDate hoje) {
        return jdbc.queryForObject("""
                SELECT GREATEST(
                  (SELECT COALESCE(MAX(quantidade_hospedes), 0) FROM reserva
                    WHERE tipo_quarto_id = ? AND situacao = 'CONFIRMADA' AND data_saida_prevista > ?),
                  (SELECT COALESCE(MAX(n), 0) FROM (
                     SELECT COUNT(*) AS n FROM hospedagem h
                     JOIN quarto q ON q.id = h.quarto_id
                     JOIN hospedagem_hospede hh ON hh.hospedagem_id = h.id
                     WHERE q.tipo_quarto_id = ? AND h.situacao = 'ATIVA' GROUP BY h.id) x))""",
                Integer.class, tipoId, hoje, tipoId);
    }

    static TipoQuarto ler(ResultSet rs, String prefixo) throws SQLException {
        return new TipoQuarto(rs.getInt(prefixo + "id"), rs.getString(prefixo + "codigo"),
                rs.getString(prefixo + "descricao"), rs.getInt(prefixo + "capacidade_maxima"),
                rs.getBigDecimal(prefixo + "valor_diaria"));
    }
}
