package hospedabem.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InicializadorBanco {
    private final JdbcTemplate jdbc;
    private final boolean demonstracao;

    public InicializadorBanco(JdbcTemplate jdbc, @Value("${hb.demo:false}") boolean demonstracao) {
        this.jdbc = jdbc;
        this.demonstracao = demonstracao;
    }

    /** Cria a carga somente em banco vazio e aplica migrações, serializando o DDL entre estações. */
    @Transactional
    public boolean inicializarSeVazio() {
        // Serializa o DDL entre estações; o bloqueio é liberado no commit ou rollback.
        jdbc.execute("SELECT pg_advisory_xact_lock(hashtext('hospedabem.inicializacao'))");
        boolean vazio = !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT to_regclass('public.funcionario') IS NOT NULL", Boolean.class));
        if (vazio) {
            executar("db/schema.sql");
            executar("db/seed.sql");
            if (demonstracao) executar("db/demo.sql");
        }
        executar("db/migracoes/002_itens_consumo.sql");
        executarSeFaltar("db/migracoes/003_consumo.sql", "SELECT 1 FROM information_schema.columns "
                + "WHERE table_name = 'consumo' AND column_name = 'valor_total' AND is_generated = 'ALWAYS'");
        executarSeFaltar("db/migracoes/004_checkout.sql", "SELECT 1 FROM information_schema.columns "
                + "WHERE table_name = 'pagamento' AND column_name = 'valor_total' AND numeric_precision = 15");
        executarSeFaltar("db/migracoes/005_senha.sql", "SELECT 1 FROM information_schema.columns "
                + "WHERE table_name = 'funcionario' AND column_name = 'senha_provisoria'");
        executarSeFaltar("db/migracoes/006_hospedes.sql",
                "SELECT 1 FROM pg_indexes WHERE indexname = 'hospedagem_hospede_hospede'");
        return vazio;
    }

    /**
     * Executa o script só enquanto {@code consultaDeAplicada} não encontra nada: ALTER TABLE trava a
     * tabela inteira mesmo quando não muda nada, e as migrações rodam a cada inicialização de estação.
     */
    private void executarSeFaltar(String script, String consultaDeAplicada) {
        boolean aplicada = Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT EXISTS (" + consultaDeAplicada + ")", Boolean.class));
        if (!aplicada) executar(script);
    }

    public void executar(String script) {
        jdbc.execute((java.sql.Connection c) -> {
            ScriptUtils.executeSqlScript(c, new ClassPathResource(script));
            return null;
        });
    }
}
