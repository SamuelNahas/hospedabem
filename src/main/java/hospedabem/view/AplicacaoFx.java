package hospedabem.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

public class AplicacaoFx extends Application {
    public static final String ESTILO = "/hospedabem/view/estilo.css";

    private static ConfigurableApplicationContext contexto;
    private static String erroInicial;

    public static void abrir(ConfigurableApplicationContext ctx, String erro, String[] args) {
        contexto = ctx;
        erroInicial = erro;
        launch(AplicacaoFx.class, args);
    }

    @Override
    public void start(Stage palco) {
        if (erroInicial != null) {
            Alert alerta = new Alert(Alert.AlertType.ERROR, erroInicial);
            alerta.setHeaderText("HospedaBem não pôde iniciar");
            alerta.showAndWait();
            Platform.exit();
            return;
        }
        Scene cena = new Scene(new TelaLogin(contexto), 1180, 780);
        cena.getStylesheets().add(AplicacaoFx.class.getResource(ESTILO).toExternalForm());
        palco.setTitle("HospedaBem");
        palco.setMinWidth(900);
        palco.setMinHeight(600);
        palco.setScene(cena);
        palco.show();
    }

    @Override
    public void stop() {
        contexto.close();
    }
}
