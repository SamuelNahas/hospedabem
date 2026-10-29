package hospedabem;

import hospedabem.dao.InicializadorBanco;
import hospedabem.view.AplicacaoFx;
import hospedabem.view.Mensagens;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * HospedaBem - Iteração 1 - Grupo N. Aplicação desktop JavaFX sobre Spring Boot (sem servidor web).
 * Esta classe não estende Application, o que permite rodar o JavaFX a partir do classpath (java -jar).
 */
@SpringBootApplication
public class App {

    @Bean
    Clock relogio() {
        return Clock.systemDefaultZone();
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
