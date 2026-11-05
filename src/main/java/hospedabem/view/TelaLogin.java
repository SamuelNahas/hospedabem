package hospedabem.view;

import hospedabem.controller.ControladorAutenticacao;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ConfigurableApplicationContext;

public class TelaLogin extends StackPane {

    public TelaLogin(ConfigurableApplicationContext contexto) {
        ControladorAutenticacao controlador = contexto.getBean(ControladorAutenticacao.class);
        Aviso aviso = new Aviso();
        TextField matricula = new TextField();
        matricula.setPromptText("ex.: recep01");
        PasswordField senha = new PasswordField();
        Button entrar = Campos.botao("Entrar", "botao-primario");
        entrar.setDefaultButton(true);
        entrar.setMaxWidth(Double.MAX_VALUE);

        entrar.setOnAction(e -> {
            String m = matricula.getText();
            char[] s = senha.getText().toCharArray();
            senha.clear();
            Tarefa.executar(entrar, aviso, () -> controlador.autenticar(m, s), funcionario -> {
                if (!controlador.senhaVencida()) {
                    getScene().setRoot(new TelaPrincipal(contexto, funcionario));
                    return;
                }
                // RNF-8: com a senha vencida, só a troca é liberada antes do menu.
                var cena = getScene();
                TelaTrocaSenha troca = new TelaTrocaSenha(controlador, true,
                        () -> cena.setRoot(new TelaPrincipal(contexto, funcionario)));
                VBox raiz = new VBox(troca.getAviso(), troca);
                raiz.getStyleClass().add("conteudo");
                getScene().setRoot(raiz);
            });
        });

        Label marca = new Label("HospedaBem");
        marca.getStyleClass().add("marca-login");
        Label subtitulo = new Label("Sistema de hospedagem · acesso de funcionários");
        subtitulo.getStyleClass().add("subtitulo-pagina");
        VBox cartao = new VBox(marca, subtitulo, aviso,
                Campos.formulario("Matrícula", matricula, "Senha", senha), entrar);
        cartao.getStyleClass().addAll("cartao", "cartao-login");
        cartao.setMaxWidth(380);
        cartao.setMaxHeight(VBox.USE_PREF_SIZE);
        getChildren().add(cartao);
        setAlignment(Pos.CENTER);
        getStyleClass().add("fundo-login");
    }
}
