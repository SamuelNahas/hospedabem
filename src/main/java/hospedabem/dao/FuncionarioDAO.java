package hospedabem.dao;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class FuncionarioDAO {
    private static final String COLUNAS = "id, matricula, cpf, nome, endereco, cidade, estado, telefone, email, "
            + "data_contratacao, perfil";

    private final JdbcTemplate jdbc;

    public FuncionarioDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Credencial(Funcionario funcionario, String senhaHash) {
    }

    /** A matrícula é gravada em minúsculas, então o login compara em minúsculas: "Maria01" entra. */
    public Optional<Credencial> buscarCredencial(String matricula) {
        return jdbc.query("SELECT " + COLUNAS + ", senha_hash FROM funcionario WHERE lower(matricula) = ?",
                (rs, i) -> new Credencial(ler(rs), rs.getString("senha_hash")), normalizar(matricula))
                .stream().findFirst();
    }

    public List<Funcionario> listar() {
        return jdbc.query("SELECT " + COLUNAS + " FROM funcionario ORDER BY matricula", (rs, i) -> ler(rs));
    }

    public Optional<Funcionario> buscarPorId(int id) {
        return jdbc.query("SELECT " + COLUNAS + " FROM funcionario WHERE id = ?", (rs, i) -> ler(rs), id)
                .stream().findFirst();
    }

    /** Busca por termo em matrícula, nome ou CPF, para a busca da tela. */
    public List<Funcionario> procurar(String termo) {
        String limpo = termo == null ? "" : termo.trim().toLowerCase();
        String padrao = "%" + limpo + "%";
        // O padrão do CPF só vale quando o termo é um número (com ou sem pontuação); assim um nome
        // ou uma matrícula com dígitos não casa por acidente com o CPF de outro funcionário.
        String digitos = limpo.replaceAll("[.\\-/ ]", "");
        String padraoCpf = digitos.isEmpty() || !digitos.chars().allMatch(Character::isDigit)
                ? "-" : "%" + digitos + "%";
        return jdbc.query("SELECT " + COLUNAS + " FROM funcionario WHERE lower(matricula) LIKE ? "
                + "OR lower(nome) LIKE ? OR replace(replace(cpf, '.', ''), '-', '') LIKE ? ORDER BY matricula",
                (rs, i) -> ler(rs), padrao, padrao, padraoCpf);
    }

    public boolean existeMatricula(String matricula, int exceto) {
        return contar("matricula = ? AND id <> ?", matricula, exceto) > 0;
    }

    public boolean existeCpf(String cpf, int exceto) {
        return contar("cpf = ? AND id <> ?", cpf, exceto) > 0;
    }

    /**
     * Trava as linhas dos administradores até o fim da transação. Sem isso, duas estações rebaixando
     * ou excluindo administradores ao mesmo tempo contam uma à outra e o hotel fica sem nenhum.
     */
    public void bloquearAdministradores() {
        jdbc.queryForList("SELECT id FROM funcionario WHERE perfil = 'ADMINISTRADOR' ORDER BY id FOR UPDATE");
    }

    /** Reservas e hospedagens registradas pelo funcionário; a exclusão não pode apagar esse histórico. */
    public int contarVinculos(int id) {
        return jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM reserva WHERE funcionario_id = ?) "
                + "+ (SELECT COUNT(*) FROM hospedagem WHERE funcionario_id = ?)", Integer.class, id, id);
    }

    public int inserir(Funcionario f, String senhaHash) {
        return jdbc.queryForObject("INSERT INTO funcionario (matricula, cpf, nome, endereco, cidade, estado, "
                        + "telefone, email, data_contratacao, perfil, senha_hash) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id", Integer.class,
                f.matricula(), f.cpf(), f.nome(), f.endereco(), f.cidade(), f.estado(), f.telefone(), f.email(),
                f.dataContratacao(), f.perfil().name(), senhaHash);
    }

    public int atualizar(Funcionario f) {
        return jdbc.update("UPDATE funcionario SET matricula = ?, cpf = ?, nome = ?, endereco = ?, cidade = ?, "
                        + "estado = ?, telefone = ?, email = ?, data_contratacao = ?, perfil = ? WHERE id = ?",
                f.matricula(), f.cpf(), f.nome(), f.endereco(), f.cidade(), f.estado(), f.telefone(), f.email(),
                f.dataContratacao(), f.perfil().name(), f.id());
    }

    /** Grava só o hash e registra a data da troca, usada pela renovação periódica (RNF-8). */
    public int atualizarSenha(int id, String senhaHash) {
        return jdbc.update("UPDATE funcionario SET senha_hash = ?, senha_alterada_em = now() WHERE id = ?",
                senhaHash, id);
    }

    public int excluir(int id) {
        return jdbc.update("DELETE FROM funcionario WHERE id = ?", id);
    }

    private static String normalizar(String matricula) {
        return matricula == null ? "" : matricula.trim().toLowerCase();
    }

    private int contar(String condicao, Object... argumentos) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM funcionario WHERE " + condicao, Integer.class, argumentos);
    }

    /**
     * Reconstitui a linha como está gravada, sem as validações de formato de
     * {@link Funcionario#informado}: uma linha antiga fora do formato atual não pode impedir a
     * listagem nem o login de quem está nela.
     */
    private static Funcionario ler(ResultSet rs) throws SQLException {
        return new Funcionario(rs.getInt("id"), rs.getString("matricula"), rs.getString("cpf"), rs.getString("nome"),
                rs.getString("endereco"), rs.getString("cidade"), rs.getString("estado"), rs.getString("telefone"),
                rs.getString("email"), rs.getDate("data_contratacao").toLocalDate(),
                Perfil.valueOf(rs.getString("perfil")));
    }
}
