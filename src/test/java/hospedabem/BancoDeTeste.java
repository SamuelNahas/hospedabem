package hospedabem;

import hospedabem.dao.InicializadorBanco;
import org.springframework.jdbc.core.JdbcTemplate;

/** Recria o esquema e a carga fictícia do banco de teste antes de cada caso. */
public final class BancoDeTeste {
    private BancoDeTeste() {
    }

    public static void recriar(JdbcTemplate jdbc, InicializadorBanco inicializador) {
        String banco = jdbc.queryForObject("SELECT current_database()", String.class);
        if (!banco.endsWith("_test"))
            throw new IllegalStateException("Recusado: testes só recriam bancos *_test, não " + banco);
        jdbc.execute("DROP SCHEMA public CASCADE");
        jdbc.execute("CREATE SCHEMA public");
        inicializador.inicializarSeVazio();
    }
}
