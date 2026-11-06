package hospedabem.dao;

import hospedabem.model.ContaDaHospedagem;
import hospedabem.model.Hospedagem;
import hospedabem.model.Hospede;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class HospedagemDAO {
    private final JdbcTemplate jdbc;

    public HospedagemDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
}
