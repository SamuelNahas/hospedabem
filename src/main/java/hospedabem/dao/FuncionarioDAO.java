package hospedabem.dao;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class FuncionarioDAO {
    private final JdbcTemplate jdbc;

    public FuncionarioDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Credencial(Funcionario funcionario, String senhaHash) {
    }

    public Optional<Credencial> buscarCredencial(String matricula) {
        return jdbc.query("SELECT id, matricula, cpf, nome, data_contratacao, perfil, senha_hash FROM funcionario WHERE matricula = ?",
                (rs, i) -> new Credencial(new Funcionario(rs.getInt("id"), rs.getString("matricula"), rs.getString("cpf"),
                        rs.getString("nome"), rs.getDate("data_contratacao").toLocalDate(),
                        Perfil.valueOf(rs.getString("perfil"))), rs.getString("senha_hash")),
                matricula).stream().findFirst();
    }
}
