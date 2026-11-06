package hospedabem.view;

import hospedabem.controller.ControladorAutenticacao;
import hospedabem.model.PoliticaDeSenha;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

/** RNF-8: troca da própria senha, obrigatória quando a senha está vencida ou é provisória. */
public class TelaTrocaSenha extends Pagina {

    public TelaTrocaSenha(ControladorAutenticacao controlador, boolean obrigatoria, Runnable aoConcluir) {
        super("Trocar senha", obrigatoria
                ? "Sua senha venceu ou foi definida pelo administrador. Defina uma nova senha para continuar."
                : "Altere a sua senha de acesso.");
        PasswordField atual = new PasswordField();
        PasswordField nova = new PasswordField();
        PasswordField confirmacao = new PasswordField();
        Label regras = new Label("A nova senha deve ter ao menos " + PoliticaDeSenha.MINIMO_DE_CARACTERES
                + " caracteres, com letras e números, e ser diferente da atual. Ela vale "
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
                if (aoConcluir != null) aoConcluir.run();
            });
        });
        getChildren().add(Campos.cartao("Nova senha", regras,
                Campos.formulario("Senha atual", atual, "Nova senha", nova, "Confirme a nova senha", confirmacao),
                Campos.acoes(trocar)));
    }
}
