package hospedabem.view;

import hospedabem.controller.ControladorAutenticacao;
import hospedabem.model.PoliticaDeSenha;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/** RNF-8: troca da própria senha, pelo menu ou obrigatória quando a senha está vencida ou é provisória. */
public class TelaTrocaSenha extends Pagina {

    public static TelaTrocaSenha voluntaria(ControladorAutenticacao controlador) {
        return new TelaTrocaSenha(controlador, "Altere a sua senha de acesso.", () -> { });
    }

    /** Raiz da cena depois de um login com a senha vencida: só é possível trocar a senha ou sair. */
    public static Parent obrigatoria(ControladorAutenticacao controlador, Runnable aoConcluir, Runnable aoSair) {
        TelaTrocaSenha troca = new TelaTrocaSenha(controlador,
                "Sua senha venceu ou foi definida pelo administrador. Defina uma nova senha para continuar.",
                aoConcluir);
        Button sair = Campos.botao("Sair", "botao-sair");
        sair.setOnAction(e -> {
            controlador.sair();
            aoSair.run();
        });
        VBox raiz = new VBox(troca.getAviso(), troca, Campos.acoes(sair));
        raiz.getStyleClass().add("conteudo");
        return raiz;
    }

    private TelaTrocaSenha(ControladorAutenticacao controlador, String subtitulo, Runnable aoConcluir) {
        super("Trocar senha", subtitulo);
        PasswordField atual = new PasswordField();
        PasswordField nova = new PasswordField();
        PasswordField confirmacao = new PasswordField();
        Label regras = new Label("A nova senha deve ter ao menos " + PoliticaDeSenha.MINIMO_DE_CARACTERES
                + " caracteres, sem contar espaços, com letras e números, e ser diferente da atual. Ela vale "
                + PoliticaDeSenha.VALIDADE_EM_DIAS + " dias.");
        regras.setWrapText(true);
        Button trocar = Campos.botao("Trocar senha", "botao-primario");
        trocar.setDefaultButton(true);
        trocar.setOnAction(e -> {
            char[] a = atual.getText().toCharArray();
            char[] n = nova.getText().toCharArray();
            char[] c = confirmacao.getText().toCharArray();
            atual.clear();
            nova.clear();
            confirmacao.clear();
            Tarefa.executar(trocar, aviso, () -> {
                controlador.trocarSenha(a, n, c);
                return true;
            }, ok -> {
                aviso.sucesso("Senha alterada.");
                aoConcluir.run();
            });
        });
        getChildren().add(Campos.cartao("Nova senha", regras,
                Campos.formulario("Senha atual", atual, "Nova senha", nova, "Confirme a nova senha", confirmacao),
                Campos.acoes(trocar)));
    }
}
