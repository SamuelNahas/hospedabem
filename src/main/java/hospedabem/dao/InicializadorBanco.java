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
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('public.funcionario') IS NOT NULL", Boolean.class)))
            return false;
        executar("db/schema.sql");
        executar("db/seed.sql");
        if (demonstracao) executar("db/demo.sql");
        return true;
    }

    public void executar(String script) {
        jdbc.execute((java.sql.Connection c) -> {
            ScriptUtils.executeSqlScript(c, new ClassPathResource(script));
            return null;
        });
    }
}
