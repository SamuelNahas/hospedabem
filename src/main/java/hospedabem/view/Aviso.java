package hospedabem.view;

import javafx.scene.control.Label;

/** Faixa de mensagem no topo da página: erro ou sucesso, sem janelas modais. */
public class Aviso extends Label {

    public Aviso() {
        setWrapText(true);
        setMaxWidth(Double.MAX_VALUE);
        limpar();
    }

    public void erro(String mensagem) {
        mostrar(mensagem, "aviso-erro");
    }

    public void sucesso(String mensagem) {
        mostrar(mensagem, "aviso-sucesso");
    }

    public void limpar() {
        setText("");
        setVisible(false);
        setManaged(false);
    }

    private void mostrar(String mensagem, String estilo) {
        getStyleClass().setAll("label", "aviso", estilo);
        setText(mensagem);
        setVisible(true);
        setManaged(true);
    }
}
