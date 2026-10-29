package hospedabem.dao;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InicializadorBanco {
    private final JdbcTemplate jdbc;

    public InicializadorBanco(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public boolean inicializarSeVazio() {
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('public.funcionario') IS NOT NULL", Boolean.class)))
            return false;
        executar("db/schema.sql");
        executar("db/seed.sql");
        return true;
    }

    public void executar(String script) {
        jdbc.execute((java.sql.Connection c) -> {
            ScriptUtils.executeSqlScript(c, new ClassPathResource(script));
            return null;
        });
    }
}
