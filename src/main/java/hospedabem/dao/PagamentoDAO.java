package hospedabem.dao;

import hospedabem.model.Extrato;
import hospedabem.model.Pagamento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PagamentoDAO {
    private final JdbcTemplate jdbc;

    public PagamentoDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** A restrição UNIQUE da conta impede um segundo pagamento da mesma conta (UC12). */
    public void inserir(int contaId, Pagamento p, int funcionarioId) {
        Extrato e = p.extrato();
        jdbc.update("""
                INSERT INTO pagamento (conta_hospedagem_id, forma, quantidade_diarias, valor_diarias, valor_consumos,
                                       valor_total, confirmado_em, funcionario_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                contaId, p.forma().name(), e.quantidadeDiarias(), e.valorDiarias(), e.valorConsumos(), e.total(),
                p.confirmadoEm(), funcionarioId);
    }
}
