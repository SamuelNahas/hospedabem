package hospedabem;

import hospedabem.dao.InicializadorBanco;
import hospedabem.view.Mensagens;
import hospedabem.view.TelaLogin;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.time.Clock;

/** HospedaBem - Iteração 1 - Grupo N. Aplicação desktop Swing sobre Spring Boot (sem servidor web). */
@SpringBootApplication
public class App {

    @Bean
    Clock relogio() {
        return Clock.systemDefaultZone();
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext contexto = new SpringApplicationBuilder(App.class).headless(false).run(args);
        try {
            if (contexto.getBean(InicializadorBanco.class).inicializarSeVazio())
                System.out.println("Banco vazio: esquema e carga fictícia criados.");
        } catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, Mensagens.de(e), "HospedaBem", JOptionPane.ERROR_MESSAGE);
            contexto.close();
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> new TelaLogin(contexto).setVisible(true));
    }
}
