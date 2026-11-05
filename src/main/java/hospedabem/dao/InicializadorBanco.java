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
        return vazio;
    }

    public void executar(String script) {
        jdbc.execute((java.sql.Connection c) -> {
            ScriptUtils.executeSqlScript(c, new ClassPathResource(script));
            return null;
        });
    }
}
