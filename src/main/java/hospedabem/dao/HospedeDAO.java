package hospedabem.dao;

import hospedabem.model.Hospede;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class HospedeDAO {
    private static final String COLUNAS = "id, cpf_ou_passaporte, nome, data_nascimento, endereco, cidade, estado, "
            + "telefone, email";

    private final JdbcTemplate jdbc;

    public HospedeDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Hospede> listar() {
        return jdbc.query("SELECT " + COLUNAS + " FROM hospede ORDER BY nome", (rs, i) -> ler(rs, ""));
    }

    public Optional<Hospede> buscarPorDocumento(String cpfOuPassaporte) {
        return jdbc.query("SELECT " + COLUNAS + " FROM hospede WHERE cpf_ou_passaporte = ?", (rs, i) -> ler(rs, ""),
                normalizar(cpfOuPassaporte)).stream().findFirst();
    }

    public Optional<Hospede> buscarPorId(int id) {
        return jdbc.query("SELECT " + COLUNAS + " FROM hospede WHERE id = ?", (rs, i) -> ler(rs, ""), id)
                .stream().findFirst();
    }

    /**
     * Busca por termo em documento ou nome, para a busca da tela. O padrão do documento usa a grafia
     * normalizada, então "111.111.111-11" encontra a linha gravada como "11111111111".
     */
    public List<Hospede> procurar(String termo) {
        String padraoNome = "%" + (termo == null ? "" : termo.trim().toLowerCase(Locale.ROOT)) + "%";
        String documento = normalizar(termo);
        String padraoDocumento = documento.isEmpty() ? "%" : "%" + documento + "%";
        return jdbc.query("SELECT " + COLUNAS + " FROM hospede WHERE lower(nome) LIKE ? "
                        + "OR cpf_ou_passaporte LIKE ? ORDER BY nome", (rs, i) -> ler(rs, ""),
                padraoNome, padraoDocumento);
    }

    public boolean existeDocumento(String cpfOuPassaporte, int exceto) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM hospede WHERE cpf_ou_passaporte = ? AND id <> ?",
                Integer.class, normalizar(cpfOuPassaporte), exceto) > 0;
    }

    /**
     * Reservas em que o hóspede é responsável e hospedagens em que ele consta: a exclusão não pode
     * apagar esse histórico.
     */
    public int contarVinculos(int id) {
        return jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM reserva WHERE hospede_responsavel_id = ?) "
                + "+ (SELECT COUNT(*) FROM hospedagem_hospede WHERE hospede_id = ?)", Integer.class, id, id);
    }

    public int inserir(Hospede h) {
        return jdbc.queryForObject("INSERT INTO hospede (cpf_ou_passaporte, nome, data_nascimento, endereco, "
                        + "cidade, estado, telefone, email) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Integer.class, h.cpfOuPassaporte(), h.nome(), h.dataNascimento(), h.endereco(), h.cidade(),
                h.estado(), h.telefone(), h.email());
    }

    public int atualizar(Hospede h) {
        return jdbc.update("UPDATE hospede SET cpf_ou_passaporte = ?, nome = ?, data_nascimento = ?, endereco = ?, "
                        + "cidade = ?, estado = ?, telefone = ?, email = ? WHERE id = ?",
                h.cpfOuPassaporte(), h.nome(), h.dataNascimento(), h.endereco(), h.cidade(), h.estado(),
                h.telefone(), h.email(), h.id());
    }

    public int excluir(int id) {
        return jdbc.update("DELETE FROM hospede WHERE id = ?", id);
    }

    /** Mesma normalização do domínio: a busca e a gravação precisam enxergar a mesma grafia. */
    public static String normalizar(String documento) {
        return Hospede.normalizarDocumento(documento);
    }

    /**
     * Reconstitui a linha como está gravada, sem as validações de formato de
     * {@link Hospede#informado}: os documentos fictícios da carga inicial não passam pelos dígitos
     * verificadores e não podem impedir a listagem nem o atendimento de quem está nelas.
     */
    static Hospede ler(ResultSet rs, String prefixo) throws SQLException {
        return new Hospede(rs.getInt(prefixo + "id"), rs.getString(prefixo + "cpf_ou_passaporte"),
                rs.getString(prefixo + "nome"), rs.getDate(prefixo + "data_nascimento").toLocalDate(),
                rs.getString(prefixo + "endereco"), rs.getString(prefixo + "cidade"), rs.getString(prefixo + "estado"),
                rs.getString(prefixo + "telefone"), rs.getString(prefixo + "email"));
    }
}
