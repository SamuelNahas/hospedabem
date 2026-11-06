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
        // ALTER TABLE trava a tabela inteira a cada execução: só roda enquanto a migração faltar.
        if (!existe("SELECT 1 FROM information_schema.columns WHERE table_name = 'consumo' "
                + "AND column_name = 'valor_total' AND is_generated = 'ALWAYS'"))
            executar("db/migracoes/003_consumo.sql");
        return vazio;
    }

    private boolean existe(String consulta) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (" + consulta + ")", Boolean.class));
    }

    public void executar(String script) {
        jdbc.execute((java.sql.Connection c) -> {
            ScriptUtils.executeSqlScript(c, new ClassPathResource(script));
            return null;
        });
    }
}
