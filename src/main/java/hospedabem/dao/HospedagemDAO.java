package hospedabem.dao;

import hospedabem.model.ContaDaHospedagem;
import hospedabem.model.Hospedagem;
import hospedabem.model.Hospede;
import hospedabem.model.Quarto;
import hospedabem.model.ResumoHospedagem;
import hospedabem.model.SituacaoConta;
import hospedabem.model.SituacaoHospedagem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class HospedagemDAO {
    private static final String SELECT = """
            SELECT h.id, h.codigo, h.data_hora_entrada, h.data_saida_prevista, h.data_hora_saida, h.situacao,
                   h.quarto_id, c.id AS c_id, c.situacao AS c_situacao, c.valor_diaria AS c_valor_diaria
            FROM hospedagem h JOIN conta_hospedagem c ON c.hospedagem_id = h.id
            WHERE h.codigo = ?""";

    private final JdbcTemplate jdbc;
    private final QuartoDAO quartoDAO;
    private final ConsumoDAO consumoDAO;

    public HospedagemDAO(JdbcTemplate jdbc, QuartoDAO quartoDAO, ConsumoDAO consumoDAO) {
        this.jdbc = jdbc;
        this.quartoDAO = quartoDAO;
        this.consumoDAO = consumoDAO;
    }

    public void salvar(Hospedagem h, int funcionarioId) {
        Map<String, Object> gerado = jdbc.queryForMap("""
                INSERT INTO hospedagem (data_hora_entrada, data_saida_prevista, situacao, quarto_id, reserva_id, funcionario_id)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id, codigo""",
                h.getDataHoraEntrada(), h.getDataSaidaPrevista(), h.getSituacao().name(), h.getQuarto().getId(),
                h.getReserva() == null ? null : h.getReserva().getId(), funcionarioId);
        h.registrarIdentificacao((Integer) gerado.get("id"), (Integer) gerado.get("codigo"));
        for (Hospede hp : h.getHospedes()) {
            jdbc.update("INSERT INTO hospedagem_hospede (hospedagem_id, hospede_id) VALUES (?, ?)", h.getId(), hp.id());
        }
        ContaDaHospedagem c = h.getConta();
        c.setId(jdbc.queryForObject("INSERT INTO conta_hospedagem (hospedagem_id, situacao, valor_diaria) VALUES (?, ?, ?) RETURNING id",
                Integer.class, h.getId(), c.getSituacao().name(), c.getValorDiaria()));
    }

    /** Hospedagens ativas com quarto e nomes dos hóspedes, para a localização na tela. */
    public List<ResumoHospedagem> listarAtivas() {
        return jdbc.query("""
                SELECT h.codigo, q.numero, h.data_hora_entrada, h.data_saida_prevista,
                       (SELECT string_agg(p.nome, ', ' ORDER BY p.nome) FROM hospedagem_hospede hh
                          JOIN hospede p ON p.id = hh.hospede_id WHERE hh.hospedagem_id = h.id) AS hospedes
                FROM hospedagem h JOIN quarto q ON q.id = h.quarto_id
                WHERE h.situacao = 'ATIVA' ORDER BY q.numero""",
                (rs, i) -> new ResumoHospedagem(rs.getInt("codigo"), rs.getInt("numero"),
                        rs.getString("hospedes") == null ? "" : rs.getString("hospedes"),
                        rs.getTimestamp("data_hora_entrada").toLocalDateTime(),
                        rs.getDate("data_saida_prevista").toLocalDate()));
    }

    /** Hospedagem com quarto, conta e consumos, sem bloqueio. */
    public Optional<Hospedagem> buscarPorCodigo(int codigo) {
        return buscarLinha(codigo, "").map(l -> {
            consumoDAO.listarPorConta(l.conta().getId()).forEach(l.conta()::adicionarRegistrado);
            return l.reconstituir(quartoDAO.buscarPorId(l.quartoId()).orElseThrow());
        });
    }

    /**
     * Hospedagem e conta travadas até o fim da transação, sem quarto nem consumos: só o necessário
     * para o lançamento (UC11). O check-out (UC12) trava as mesmas linhas, então um espera o outro:
     * nenhum consumo entra depois que o check-out leu o total, e nenhum check-out fecha a conta no
     * meio de um lançamento.
     */
    public Optional<Hospedagem> buscarParaLancamento(int codigo) {
        return buscarLinha(codigo, " FOR UPDATE OF h, c").map(l -> l.reconstituir(null));
    }

    private record Linha(int id, int codigo, LocalDateTime entrada, LocalDate saidaPrevista,
                         SituacaoHospedagem situacao, LocalDateTime saida, int quartoId, ContaDaHospedagem conta) {
        Hospedagem reconstituir(Quarto quarto) {
            return Hospedagem.reconstituir(id, codigo, entrada, saidaPrevista, situacao, saida, quarto, conta);
        }
    }

    private Optional<Linha> buscarLinha(int codigo, String trava) {
        return jdbc.query(SELECT + trava, (rs, i) -> {
            Timestamp saida = rs.getTimestamp("data_hora_saida");
            return new Linha(rs.getInt("id"), rs.getInt("codigo"), rs.getTimestamp("data_hora_entrada").toLocalDateTime(),
                    rs.getDate("data_saida_prevista").toLocalDate(), SituacaoHospedagem.valueOf(rs.getString("situacao")),
                    saida == null ? null : saida.toLocalDateTime(), rs.getInt("quarto_id"),
                    new ContaDaHospedagem(rs.getInt("c_id"), SituacaoConta.valueOf(rs.getString("c_situacao")),
                            rs.getBigDecimal("c_valor_diaria")));
        }, codigo).stream().findFirst();
    }
}
