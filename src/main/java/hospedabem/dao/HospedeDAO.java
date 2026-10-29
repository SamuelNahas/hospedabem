package hospedabem.dao;

import hospedabem.model.Hospede;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Somente consulta: o gerenciamento de hóspedes (UC06) é da iteração 2. */
@Repository
public class HospedeDAO {
    private final JdbcTemplate jdbc;

    public HospedeDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Hospede> buscarPorDocumento(String cpfOuPassaporte) {
        return jdbc.query("SELECT * FROM hospede WHERE cpf_ou_passaporte = ?", (rs, i) -> ler(rs, ""),
                normalizar(cpfOuPassaporte)).stream().findFirst();
    }

    /** CPF é gravado só com dígitos; passaporte, em maiúsculas e sem separadores. */
    public static String normalizar(String documento) {
        return documento == null ? "" : documento.replaceAll("[\\s.\\-/]", "").toUpperCase();
    }

    static Hospede ler(ResultSet rs, String prefixo) throws SQLException {
        return new Hospede(rs.getInt(prefixo + "id"), rs.getString(prefixo + "cpf_ou_passaporte"),
                rs.getString(prefixo + "nome"), rs.getDate(prefixo + "data_nascimento").toLocalDate(),
                rs.getString(prefixo + "endereco"), rs.getString(prefixo + "cidade"), rs.getString(prefixo + "estado"),
                rs.getString(prefixo + "telefone"), rs.getString(prefixo + "email"));
    }
}
