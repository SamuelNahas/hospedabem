package hospedabem.dao;

import hospedabem.model.Periodo;
import hospedabem.model.Reserva;
import hospedabem.model.SituacaoReserva;
import hospedabem.model.SolicitacaoReserva;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public class ReservaDAO {
    private static final String SELECT = """
            SELECT r.id, r.codigo, r.data_entrada_prevista, r.data_saida_prevista, r.quantidade_hospedes,
                   r.situacao, r.funcionario_id, r.versao,
                   h.id AS h_id, h.cpf_ou_passaporte AS h_cpf_ou_passaporte, h.nome AS h_nome,
                   h.data_nascimento AS h_data_nascimento, h.endereco AS h_endereco, h.cidade AS h_cidade,
                   h.estado AS h_estado, h.telefone AS h_telefone, h.email AS h_email,
                   t.id AS t_id, t.codigo AS t_codigo, t.descricao AS t_descricao,
                   t.capacidade_maxima AS t_capacidade_maxima, t.valor_diaria AS t_valor_diaria
            FROM reserva r
            JOIN hospede h ON h.id = r.hospede_responsavel_id
            JOIN tipo_quarto t ON t.id = r.tipo_quarto_id
            WHERE r.codigo = ?
            """;
    private static final RowMapper<Reserva> MAPA = (rs, i) -> new Reserva(rs.getInt("id"), rs.getInt("codigo"),
            HospedeDAO.ler(rs, "h_"), TipoQuartoDAO.ler(rs, "t_"),
            new Periodo(rs.getObject("data_entrada_prevista", LocalDate.class),
                    rs.getObject("data_saida_prevista", LocalDate.class)),
            rs.getInt("quantidade_hospedes"), SituacaoReserva.valueOf(rs.getString("situacao")),
            rs.getInt("funcionario_id"), rs.getInt("versao"));

    private final JdbcTemplate jdbc;

    public ReservaDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Reserva> buscarPorCodigo(int codigo) {
        return jdbc.query(SELECT, MAPA, codigo).stream().findFirst();
    }

    public Optional<Reserva> buscarPorCodigoParaAtualizar(int codigo) {
        return jdbc.query(SELECT + " FOR UPDATE OF r", MAPA, codigo).stream().findFirst();
    }

    public int inserir(SolicitacaoReserva s, int funcionarioId) {
        return jdbc.queryForObject("""
                INSERT INTO reserva (hospede_responsavel_id, tipo_quarto_id, data_entrada_prevista, data_saida_prevista,
                                     quantidade_hospedes, situacao, funcionario_id)
                VALUES (?, ?, ?, ?, ?, 'CONFIRMADA', ?) RETURNING codigo""",
                Integer.class, s.responsavel().id(), s.tipo().id(), s.periodo().entrada(), s.periodo().saida(),
                s.quantidadeHospedes(), funcionarioId);
    }

    public void atualizar(Reserva r) {
        jdbc.update("UPDATE reserva SET situacao = ?, versao = versao + 1 WHERE id = ?", r.getSituacao().name(), r.getId());
    }
}
