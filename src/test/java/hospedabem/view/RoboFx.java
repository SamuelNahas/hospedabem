package hospedabem.view;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Opera as telas JavaFX reais como um usuário: localiza campos pelo rótulo e botões pelo texto,
 * aciona-os na thread do JavaFX e espera as tarefas de banco terminarem.
 * Com -Dhb.e2e.capturas=<pasta>, salva uma imagem PNG de cada passo nomeado.
 */
final class RoboFx {
    private static final long LIMITE_MS = 20_000;
    private static final String PASTA_CAPTURAS = System.getProperty("hb.e2e.capturas");

    private final Stage palco;
    private static int captura = 1;

    private RoboFx(Stage palco) {
        this.palco = palco;
    }

    static void iniciarJavaFx() throws InterruptedException {
        CountDownLatch pronto = new CountDownLatch(1);
        try {
            Platform.startup(pronto::countDown);
        } catch (IllegalStateException jaIniciado) {
            pronto.countDown();
        }
        Platform.setImplicitExit(false);
        pronto.await(10, TimeUnit.SECONDS);
    }

    static RoboFx abrir(Parent raiz) {
        Stage palco = naFx(() -> {
            Stage s = new Stage();
            Scene cena = new Scene(raiz, 1180, 820);
            cena.getStylesheets().add(AplicacaoFx.class.getResource(AplicacaoFx.ESTILO).toExternalForm());
            s.setScene(cena);
            s.show();
            return s;
        });
        RoboFx robo = new RoboFx(palco);
        robo.aguardar();
        return robo;
    }

    void fechar() {
        naFx(() -> {
            palco.close();
            return null;
        });
    }

    void escrever(String rotulo, String texto) {
        naFx(() -> {
            Node campo = campo(rotulo);
            TextInputControl entrada = campo instanceof DatePicker d ? d.getEditor() : (TextInputControl) campo;
            entrada.setText(texto);
            return null;
        });
    }

    void escreverData(String rotulo, LocalDate data) {
        escrever(rotulo, Campos.texto(data));
    }

    /** Campo sem rótulo de formulário, identificado pelo texto de dica (prompt). */
    void escreverNoCampoComDica(String dica, String texto) {
        naFx(() -> {
            TextField campo = todos(TextField.class).stream().filter(t -> dica.equals(t.getPromptText()))
                    .findFirst().orElseThrow(() -> new AssertionError("Campo com dica não encontrado: " + dica));
            campo.setText(texto);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    void escolher(String rotulo, Predicate<Object> opcao) {
        naFx(() -> {
            ComboBox<Object> combo = (ComboBox<Object>) campo(rotulo);
            combo.setValue(combo.getItems().stream().filter(opcao).findFirst()
                    .orElseThrow(() -> new AssertionError("Opção não encontrada em " + rotulo)));
            return null;
        });
    }

    void clicar(String texto) {
        naFx(() -> {
            ButtonBase botao = botao(texto);
            if (botao.isDisabled()) throw new AssertionError("Botão desabilitado: " + texto);
            botao.fire();
            return null;
        });
        aguardar();
    }

    boolean desabilitado(String texto) {
        return naFx(() -> botao(texto).isDisabled());
    }

    void selecionarLinha(int linha) {
        naFx(() -> {
            TableView<?> tabela = todos(TableView.class).get(0);
            tabela.getSelectionModel().select(linha);
            return null;
        });
    }

    int linhasDaTabela() {
        return naFx(() -> todos(TableView.class).get(0).getItems().size());
    }

    /** Texto do aviso visível da página, ou "" se não houver. */
    String aviso() {
        return naFx(() -> todos(Aviso.class).stream().filter(Node::isVisible).map(Label::getText)
                .findFirst().orElse(""));
    }

    boolean avisoForaDaRolagem() {
        return naFx(() -> todos(Aviso.class).stream().filter(Node::isVisible).allMatch(a -> {
            for (Node n = a.getParent(); n != null; n = n.getParent())
                if (n instanceof javafx.scene.control.ScrollPane) return false;
            return true;
        }));
    }

    boolean exibeTexto(String trecho) {
        return naFx(() -> todos(Label.class).stream().anyMatch(l -> l.getText() != null && l.getText().contains(trecho)));
    }

    void capturar(String nome) {
        if (PASTA_CAPTURAS == null) return;
        WritableImage imagem = naFx(() -> palco.getScene().snapshot(null));
        PixelReader pixels = imagem.getPixelReader();
        BufferedImage png = new BufferedImage((int) imagem.getWidth(), (int) imagem.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++)
            for (int x = 0; x < png.getWidth(); x++) png.setRGB(x, y, pixels.getArgb(x, y));
        try {
            File pasta = new File(PASTA_CAPTURAS);
            pasta.mkdirs();
            ImageIO.write(png, "png", new File(pasta, String.format("%02d-%s.png", captura++, nome)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Espera as tarefas de banco em andamento terminarem e a tela processar o resultado. */
    void aguardar() {
        long limite = System.currentTimeMillis() + LIMITE_MS;
        while (naFx(Tarefa::emAndamento) > 0) {
            if (System.currentTimeMillis() > limite) throw new AssertionError("Tarefas não terminaram a tempo");
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        naFx(() -> {
            palco.getScene().getRoot().applyCss();
            palco.getScene().getRoot().layout();
            return null;
        });
    }

    private Node campo(String rotulo) {
        return todos(Label.class).stream().filter(l -> rotulo.equals(l.getText()) && l.getLabelFor() != null)
                .map(Label::getLabelFor).findFirst()
                .orElseThrow(() -> new AssertionError("Campo não encontrado: " + rotulo));
    }

    private ButtonBase botao(String texto) {
        return todos(ButtonBase.class).stream().filter(b -> texto.equals(b.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("Botão não encontrado: " + texto));
    }

    private <T> List<T> todos(Class<T> tipo) {
        List<T> encontrados = new ArrayList<>();
        coletar(palco.getScene().getRoot(), tipo, encontrados);
        return encontrados;
    }

    private static <T> void coletar(Node no, Class<T> tipo, List<T> encontrados) {
        if (!no.isVisible()) return;
        if (tipo.isInstance(no)) encontrados.add(tipo.cast(no));
        if (no instanceof Parent pai) {
            for (Node filho : pai.getChildrenUnmodifiable()) coletar(filho, tipo, encontrados);
        }
        if (no instanceof javafx.scene.control.ScrollPane rolagem && rolagem.getContent() != null)
            coletar(rolagem.getContent(), tipo, encontrados);
    }

    static <T> T naFx(Callable<T> acao) {
        if (Platform.isFxApplicationThread()) {
            try {
                return acao.call();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        FutureTask<T> tarefa = new FutureTask<>(acao);
        Platform.runLater(tarefa);
        try {
            return tarefa.get(LIMITE_MS, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof AssertionError a) throw a;
            throw new IllegalStateException(e.getCause());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
