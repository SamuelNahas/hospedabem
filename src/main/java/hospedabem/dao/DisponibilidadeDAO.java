package hospedabem.dao;

import hospedabem.model.Periodo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class DisponibilidadeDAO {
    /**
     * Estadias que consomem um quarto do tipo: reservas CONFIRMADAS (a convertida já está representada pela
     * hospedagem que originou; a cancelada não ocupa) e hospedagens ATIVAS. A hospedagem ativa ocupa até a saída
     * prevista ou, se atrasada, até amanhã: ela nunca é encerrada automaticamente.
     */
    private static final String ESTADIAS = """
            SELECT data_entrada_prevista AS entrada, data_saida_prevista AS saida FROM reserva
             WHERE tipo_quarto_id = ? AND situacao = 'CONFIRMADA' AND id <> ?
               AND data_entrada_prevista < ? AND data_saida_prevista > ?
            UNION ALL
            SELECT entrada, saida FROM (
              SELECT h.data_hora_entrada::date AS entrada, GREATEST(h.data_saida_prevista, ?::date + 1) AS saida
                FROM hospedagem h JOIN quarto q ON q.id = h.quarto_id
               WHERE q.tipo_quarto_id = ? AND h.situacao = 'ATIVA') ativas
             WHERE entrada < ? AND saida > ?
            """;

    private final JdbcTemplate jdbc;

    public DisponibilidadeDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Quartos do tipo que contam para disponibilidade: todos, exceto os em manutenção. */
    public int quartosOperacionais(int tipoId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM quarto WHERE tipo_quarto_id = ? AND situacao <> 'MANUTENCAO'",
                Integer.class, tipoId);
    }

    public List<Periodo> estadias(int tipoId, Periodo consulta, int ignorarReservaId, LocalDate hoje) {
        return jdbc.query(ESTADIAS, (rs, i) -> new Periodo(rs.getObject("entrada", LocalDate.class),
                        rs.getObject("saida", LocalDate.class)),
                tipoId, ignorarReservaId, consulta.saida(), consulta.entrada(),
                hoje, tipoId, consulta.saida(), consulta.entrada());
    }

    /** Última data comprometida por reservas confirmadas ou hospedagens ativas do tipo, se posterior a hoje. */
    public Optional<LocalDate> fimDosCompromissos(int tipoId, LocalDate hoje) {
        LocalDate fim = jdbc.queryForObject("""
                SELECT GREATEST(
                  (SELECT MAX(data_saida_prevista) FROM reserva WHERE tipo_quarto_id = ? AND situacao = 'CONFIRMADA'),
                  (SELECT MAX(GREATEST(h.data_saida_prevista, ?::date + 1)) FROM hospedagem h
                     JOIN quarto q ON q.id = h.quarto_id WHERE q.tipo_quarto_id = ? AND h.situacao = 'ATIVA'))""",
                LocalDate.class, tipoId, hoje, tipoId);
        return fim == null || !fim.isAfter(hoje) ? Optional.empty() : Optional.of(fim);
    }
}
