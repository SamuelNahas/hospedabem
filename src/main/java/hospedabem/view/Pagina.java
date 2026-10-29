package hospedabem.view;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class Pagina extends VBox {
    protected final Aviso aviso = new Aviso();

    public Pagina(String titulo, String subtitulo) {
        Label t = new Label(titulo);
        t.getStyleClass().add("titulo-pagina");
        Label s = new Label(subtitulo);
        s.getStyleClass().add("subtitulo-pagina");
        s.setWrapText(true);
        getChildren().addAll(t, s, aviso);
        getStyleClass().add("pagina");
    }
}
