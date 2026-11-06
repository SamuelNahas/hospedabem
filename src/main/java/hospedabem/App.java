package hospedabem;

import hospedabem.dao.InicializadorBanco;
import hospedabem.view.AplicacaoFx;
import hospedabem.view.Mensagens;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

// Não estende Application para o JavaFX poder ser carregado do classpath do JAR único.
@SpringBootApplication
public class App {

    /**
     * Relógio alinhado ao do banco: entrada, lançamentos, saída e validade de senha gravados por
     * estações diferentes usam a mesma referência, mesmo com o relógio de uma estação adiantado.
     * Sem banco, usa o relógio local; o erro de conexão aparece na abertura da tela.
     * ponytail: a diferença é medida uma vez, na inicialização; ajuste do relógio da estação com o
     * aplicativo aberto só vale depois de reiniciar.
     */
    @Bean
    Clock relogio(JdbcTemplate jdbc) {
        Clock local = Clock.systemDefaultZone();
        try {
            return alinhado(local, jdbc.queryForObject("SELECT LOCALTIMESTAMP", LocalDateTime.class));
        } catch (DataAccessException e) {
            LoggerFactory.getLogger(App.class).warn("Relógio do banco indisponível; usando o da estação", e);
            return local;
        }
    }

    static Clock alinhado(Clock local, LocalDateTime agoraNoBanco) {
        return Clock.offset(local, Duration.between(LocalDateTime.now(local), agoraNoBanco));
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext contexto = new SpringApplicationBuilder(App.class).run(args);
        String erro = null;
        try {
            if (contexto.getBean(InicializadorBanco.class).inicializarSeVazio())
                System.out.println("Banco vazio: esquema e carga fictícia criados.");
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(App.class).error("Falha ao preparar o banco", e);
            erro = Mensagens.de(e);
        }
        AplicacaoFx.abrir(contexto, erro, args);
    }
}
