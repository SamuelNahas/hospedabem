package hospedabem.dao;

import hospedabem.model.Quarto;
import hospedabem.model.SituacaoQuarto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class QuartoDAO {
    private static final String SELECT = """
            SELECT q.id, q.numero, q.andar, q.situacao,
                   EXISTS (SELECT 1 FROM hospedagem h WHERE h.quarto_id = q.id AND h.situacao = 'ATIVA') AS hospedado,
                   t.id AS t_id, t.codigo AS t_codigo, t.descricao AS t_descricao,
                   t.capacidade_maxima AS t_capacidade_maxima, t.valor_diaria AS t_valor_diaria
            FROM quarto q JOIN tipo_quarto t ON t.id = q.tipo_quarto_id
            """;
    private static final RowMapper<Quarto> MAPA = (rs, i) -> new Quarto(rs.getInt("id"), rs.getInt("numero"),
            rs.getInt("andar"), TipoQuartoDAO.ler(rs, "t_"), SituacaoQuarto.valueOf(rs.getString("situacao")),
            rs.getBoolean("hospedado"));

    private final JdbcTemplate jdbc;

    public QuartoDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Quarto> buscarPorNumero(int numero) {
        return jdbc.query(SELECT + " WHERE q.numero = ?", MAPA, numero).stream().findFirst();
    }

    public Optional<Quarto> buscarPorNumeroParaAtualizar(int numero) {
        return jdbc.query(SELECT + " WHERE q.numero = ? FOR UPDATE OF q", MAPA, numero).stream().findFirst();
    }

    public Optional<Quarto> buscarPorIdParaAtualizar(int id) {
        return jdbc.query(SELECT + " WHERE q.id = ? FOR UPDATE OF q", MAPA, id).stream().findFirst();
    }

    public List<Quarto> listar() {
        return jdbc.query(SELECT + " ORDER BY q.numero", MAPA);
    }

    public List<Quarto> listarElegiveis(int tipoId) {
        return jdbc.query(SELECT + """
                 WHERE t.id = ? AND q.situacao <> 'MANUTENCAO'
                   AND NOT EXISTS (SELECT 1 FROM hospedagem h WHERE h.quarto_id = q.id AND h.situacao = 'ATIVA')
                 ORDER BY q.numero""", MAPA, tipoId);
    }

    public int inserir(Quarto q) {
        return jdbc.queryForObject("INSERT INTO quarto (numero, andar, tipo_quarto_id, situacao) VALUES (?, ?, ?, ?) RETURNING id",
                Integer.class, q.getNumero(), q.getAndar(), q.getTipo().id(), q.getSituacao().name());
    }

    public void atualizar(Quarto q) {
        jdbc.update("UPDATE quarto SET numero = ?, andar = ?, tipo_quarto_id = ?, situacao = ? WHERE id = ?",
                q.getNumero(), q.getAndar(), q.getTipo().id(), q.getSituacao().name(), q.getId());
    }

    public void excluir(int id) {
        jdbc.update("DELETE FROM quarto WHERE id = ?", id);
    }

    public boolean possuiHospedagens(int quartoId) {
        return jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM hospedagem WHERE quarto_id = ?)", Boolean.class, quartoId);
    }
}
