package hospedabem.view;

import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.controller.ControladorQuarto;
import hospedabem.model.NegocioException;
import hospedabem.model.Quarto;
import hospedabem.model.SituacaoQuarto;
import hospedabem.model.TipoQuarto;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** UC04 Gerenciar Quarto. */
public class TelaQuarto extends Pagina {
    private final ControladorQuarto controlador;
    private final TableView<Quarto> tabela = Campos.tabela(
            Campos.coluna("Número", Quarto::getNumero),
            Campos.coluna("Andar", Quarto::getAndar),
            Campos.coluna("Tipo", Quarto::getTipo),
            Campos.coluna("Situação", Quarto::getSituacao));
    private final TextField numero = new TextField();
    private final TextField andar = new TextField();
    private final ComboBox<TipoQuarto> tipo = new ComboBox<>();
    private final ComboBox<SituacaoQuarto> situacao = new ComboBox<>();
    private final Button salvar = Campos.botao("Salvar", "botao-primario");
    private Quarto selecionado;

    public TelaQuarto(ControladorQuarto controlador, ControladorDisponibilidade consultas) {
        super("Quartos", "Ocupado resulta do check-in. As reservas são por tipo: o quarto físico só é escolhido "
                + "no check-in, por isso \"reservado\" aparece na consulta de disponibilidade.");
        this.controlador = controlador;
        situacao.getItems().setAll(SituacaoQuarto.values());
        situacao.setValue(SituacaoQuarto.DISPONIVEL);
        tipo.setMaxWidth(Double.MAX_VALUE);
        situacao.setMaxWidth(Double.MAX_VALUE);

        Button novo = Campos.botao("Novo", "botao-secundario");
        Button excluir = Campos.botao("Excluir", "botao-perigo");
        novo.setOnAction(e -> selecionar(null));
        salvar.setOnAction(e -> salvar());
        excluir.setOnAction(e -> excluir(excluir));
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, q) -> {
            if (q != null) selecionar(q);
        });

        Label dica = new Label("O cadastro só define Disponível ou Manutenção.");
        dica.getStyleClass().add("dica");
        getChildren().addAll(
                Campos.cartao("Quartos cadastrados", tabela),
                Campos.cartao("Dados do quarto",
                        Campos.formulario("Número", numero, "Andar", andar, "Tipo", tipo, "Situação", situacao),
                        dica, Campos.acoes(salvar, novo, excluir)));
        Tarefa.executar(novo, aviso, consultas::listarTipos, lista -> tipo.getItems().setAll(lista));
        recarregar();
    }

    private void recarregar() {
        Tarefa.executar(salvar, aviso, controlador::listar, lista -> tabela.getItems().setAll(lista));
    }

    private void selecionar(Quarto q) {
        selecionado = q;
        numero.setText(q == null ? "" : String.valueOf(q.getNumero()));
        andar.setText(q == null ? "" : String.valueOf(q.getAndar()));
        situacao.setValue(q == null ? SituacaoQuarto.DISPONIVEL : q.getSituacao());
        tipo.setValue(q == null ? null
                : tipo.getItems().stream().filter(t -> t.id() == q.getTipo().id()).findFirst().orElse(null));
        if (q == null) tabela.getSelectionModel().clearSelection();
    }

    private void salvar() {
        Quarto q;
        try {
            q = new Quarto(selecionado == null ? 0 : selecionado.getId(), Campos.inteiro(numero.getText(), "Número"),
                    Campos.inteiro(andar.getText(), "Andar"), tipo.getValue(), situacao.getValue());
        } catch (NegocioException e) {
            aviso.erro(e.getMessage());
            return;
        }
        boolean inclusao = selecionado == null;
        Tarefa.executar(salvar, aviso, () -> {
            if (inclusao) controlador.incluir(q);
            else controlador.alterar(q);
            return q;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Quarto " + q.getNumero() + " gravado.");
        });
    }

    private void excluir(Button origem) {
        if (selecionado == null) {
            aviso.erro("Selecione um quarto na lista.");
            return;
        }
        Quarto alvo = selecionado;
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Excluir o quarto " + alvo.getNumero() + "?",
                ButtonType.YES, ButtonType.NO);
        if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        Tarefa.executar(origem, aviso, () -> {
            controlador.excluir(alvo);
            return alvo;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Quarto " + alvo.getNumero() + " excluído.");
        });
    }
}
