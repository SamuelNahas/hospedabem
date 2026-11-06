package hospedabem.view;

import hospedabem.model.NegocioException;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.function.Function;
import javafx.util.StringConverter;

public final class Campos {
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private Campos() {
    }

    public static LocalDate data(String texto, String nome) {
        try {
            return LocalDate.parse(texto.trim(), DATA);
        } catch (DateTimeParseException e) {
            throw new NegocioException(nome + " inválida. Use o formato dd/mm/aaaa.");
        }
    }

    public static int inteiro(String texto, String nome) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException(nome + " deve ser um número inteiro.");
        }
    }

    public static BigDecimal decimal(String texto, String nome) {
        try {
            String t = texto.trim();
            return new BigDecimal(t.contains(",") ? t.replace(".", "").replace(',', '.') : t);
        } catch (NumberFormatException e) {
            throw new NegocioException(nome + " inválido. Exemplo: 180,00");
        }
    }

    public static DatePicker seletorData(LocalDate inicial) {
        DatePicker d = new DatePicker(inicial);
        d.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate data) {
                return texto(data);
            }

            @Override
            public LocalDate fromString(String texto) {
                try {
                    return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.trim(), DATA);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
        d.setMaxWidth(Double.MAX_VALUE);
        return d;
    }

    public static String textoData(DatePicker d) {
        return d.getEditor().getText();
    }

    public static String texto(LocalDate d) {
        return d == null ? "" : DATA.format(d);
    }

    public static String moeda(BigDecimal v) {
        return MOEDA.format(v);
    }

    public static String decimalTexto(BigDecimal v) {
        return v.toPlainString().replace('.', ',');
    }

    public static GridPane formulario(Object... rotulosECampos) {
        GridPane grade = new GridPane();
        grade.getStyleClass().add("formulario");
        ColumnConstraints rotulos = new ColumnConstraints();
        ColumnConstraints campos = new ColumnConstraints();
        campos.setHgrow(Priority.ALWAYS);
        grade.getColumnConstraints().addAll(rotulos, campos);
        for (int i = 0; i < rotulosECampos.length; i += 2) {
            Label rotulo = new Label(rotulosECampos[i].toString());
            rotulo.getStyleClass().add("rotulo");
            Node campo = (Node) rotulosECampos[i + 1];
            rotulo.setLabelFor(campo);
            grade.addRow(i / 2, rotulo, campo);
        }
        return grade;
    }

    public static VBox cartao(String titulo, Node... conteudo) {
        Label cabecalho = new Label(titulo);
        cabecalho.getStyleClass().add("titulo-cartao");
        VBox cartao = new VBox(cabecalho);
        cartao.getChildren().addAll(conteudo);
        cartao.getStyleClass().add("cartao");
        return cartao;
    }

    public static HBox acoes(Node... botoes) {
        HBox acoes = new HBox(botoes);
        acoes.getStyleClass().add("acoes");
        return acoes;
    }

    public static boolean confirmar(String pergunta) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, pergunta, ButtonType.YES, ButtonType.NO);
        return alerta.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public static Button botao(String texto, String estilo) {
        Button b = new Button(texto);
        b.getStyleClass().add(estilo);
        return b;
    }

    public static <T> TableColumn<T, Object> coluna(String titulo, Function<T, Object> valor) {
        TableColumn<T, Object> c = new TableColumn<>(titulo);
        c.setCellValueFactory(dados -> new SimpleObjectProperty<>(valor.apply(dados.getValue())));
        return c;
    }

    @SafeVarargs
    public static <T> TableView<T> tabela(TableColumn<T, Object>... colunas) {
        TableView<T> t = new TableView<>();
        t.getColumns().addAll(colunas);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPlaceholder(new Label("Nenhum registro."));
        t.setPrefHeight(220);
        return t;
    }
}
