package hospedabem.view;

import hospedabem.controller.ControladorTipoQuarto;
import hospedabem.model.NegocioException;
import hospedabem.model.TipoQuarto;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class TelaTipoQuarto extends Pagina {
    private final ControladorTipoQuarto controlador;
    private final TableView<TipoQuarto> tabela = Campos.tabela(
            Campos.coluna("Código", TipoQuarto::codigo),
            Campos.coluna("Descrição", TipoQuarto::descricao),
            Campos.coluna("Capacidade", TipoQuarto::capacidadeMaxima),
            Campos.coluna("Diária", t -> Campos.moeda(t.valorDiaria())));
    private final TextField codigo = new TextField();
    private final TextField descricao = new TextField();
    private final TextField capacidade = new TextField();
    private final TextField diaria = new TextField();
    private final Button salvar = Campos.botao("Salvar", "botao-primario");
    private TipoQuarto selecionado;

    public TelaTipoQuarto(ControladorTipoQuarto controlador) {
        super("Tipos de quarto", "Cada tipo define a capacidade máxima de hóspedes e o valor da diária.");
        this.controlador = controlador;

        Button novo = Campos.botao("Novo", "botao-secundario");
        Button excluir = Campos.botao("Excluir", "botao-perigo");
        Button buscar = Campos.botao("Buscar por código", "botao-secundario");
        novo.setOnAction(e -> selecionar(null));
        salvar.setOnAction(e -> salvar());
        excluir.setOnAction(e -> excluir(excluir));
        buscar.setOnAction(e -> {
            String c = codigo.getText();
            Tarefa.executar(buscar, aviso, () -> controlador.buscar(c), encontrado -> encontrado.ifPresentOrElse(
                    this::selecionar, () -> aviso.erro("Nenhum tipo com o código " + c + ".")));
        });
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, t) -> {
            if (t != null) selecionar(t);
        });

        getChildren().addAll(
                Campos.cartao("Tipos cadastrados", tabela),
                Campos.cartao("Dados do tipo",
                        Campos.formulario("Código", codigo, "Descrição", descricao, "Capacidade máxima", capacidade,
                                "Valor da diária (R$)", diaria),
                        Campos.acoes(salvar, novo, buscar, excluir)));
        recarregar();
    }

    private void recarregar() {
        Tarefa.executar(salvar, aviso, controlador::listar, lista -> tabela.getItems().setAll(lista));
    }

    private void selecionar(TipoQuarto t) {
        selecionado = t;
        codigo.setText(t == null ? "" : t.codigo());
        descricao.setText(t == null ? "" : t.descricao());
        capacidade.setText(t == null ? "" : String.valueOf(t.capacidadeMaxima()));
        diaria.setText(t == null ? "" : Campos.decimalTexto(t.valorDiaria()));
        if (t == null) tabela.getSelectionModel().clearSelection();
    }

    private void salvar() {
        TipoQuarto t;
        try {
            t = new TipoQuarto(selecionado == null ? 0 : selecionado.id(), codigo.getText(), descricao.getText(),
                    Campos.inteiro(capacidade.getText(), "Capacidade"), Campos.decimal(diaria.getText(), "Valor da diária"));
        } catch (NegocioException e) {
            aviso.erro(e.getMessage());
            return;
        }
        boolean inclusao = selecionado == null;
        Tarefa.executar(salvar, aviso, () -> {
            if (inclusao) controlador.incluir(t);
            else controlador.alterar(t);
            return t;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Tipo " + t.codigo() + " gravado.");
        });
    }

    private void excluir(Button origem) {
        if (selecionado == null) {
            aviso.erro("Selecione um tipo na lista.");
            return;
        }
        TipoQuarto alvo = selecionado;
        if (!Campos.confirmar("Excluir o tipo " + alvo.codigo() + "?")) return;
        Tarefa.executar(origem, aviso, () -> {
            controlador.excluir(alvo);
            return alvo;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Tipo " + alvo.codigo() + " excluído.");
        });
    }
}
