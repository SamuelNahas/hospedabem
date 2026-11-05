package hospedabem.view;

import hospedabem.controller.ControladorItemConsumo;
import hospedabem.model.ItemConsumo;
import hospedabem.model.NegocioException;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class TelaItemConsumo extends Pagina {
    private final ControladorItemConsumo controlador;
    private final TableView<ItemConsumo> tabela = Campos.tabela(
            Campos.coluna("Código", ItemConsumo::codigo),
            Campos.coluna("Descrição", ItemConsumo::descricao),
            Campos.coluna("Categoria", ItemConsumo::categoria),
            Campos.coluna("Preço", item -> Campos.moeda(item.preco())));
    private final TextField codigo = new TextField();
    private final TextField descricao = new TextField();
    private final TextField categoria = new TextField();
    private final TextField preco = new TextField();
    private final Button salvar = Campos.botao("Salvar", "botao-primario");
    private ItemConsumo selecionado;

    public TelaItemConsumo(ControladorItemConsumo controlador) {
        super("Itens de consumo e serviços", "Catálogo de itens e serviços disponíveis para as hospedagens.");
        this.controlador = controlador;
        Button limpar = Campos.botao("Limpar formulário", "botao-secundario");
        Button excluir = Campos.botao("Excluir", "botao-perigo");
        Button buscar = Campos.botao("Buscar por código", "botao-secundario");
        limpar.setOnAction(e -> selecionar(null));
        salvar.setOnAction(e -> salvar());
        excluir.setOnAction(e -> excluir(excluir));
        buscar.setOnAction(e -> {
            String c = codigo.getText();
            Tarefa.executar(buscar, aviso, () -> controlador.buscar(c), encontrado -> encontrado.ifPresentOrElse(
                    this::selecionar, () -> aviso.erro("Nenhum item com o código " + c + ".")));
        });
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, item) -> {
            if (item != null) selecionar(item);
        });
        getChildren().addAll(
                Campos.cartao("Itens cadastrados", tabela),
                Campos.cartao("Dados do item",
                        Campos.formulario("Código", codigo, "Descrição", descricao, "Categoria", categoria,
                                "Preço (R$)", preco),
                        Campos.acoes(salvar, limpar, buscar, excluir)));
        recarregar();
    }

    private void recarregar() {
        Tarefa.executar(salvar, aviso, controlador::listar, lista -> tabela.getItems().setAll(lista));
    }

    private void selecionar(ItemConsumo item) {
        selecionado = item;
        codigo.setText(item == null ? "" : item.codigo());
        descricao.setText(item == null ? "" : item.descricao());
        categoria.setText(item == null ? "" : item.categoria());
        preco.setText(item == null ? "" : Campos.decimalTexto(item.preco()));
        if (item == null) tabela.getSelectionModel().clearSelection();
    }

    private void salvar() {
        ItemConsumo item;
        try {
            item = new ItemConsumo(selecionado == null ? 0 : selecionado.id(), codigo.getText(), descricao.getText(),
                    categoria.getText(), Campos.decimal(preco.getText(), "Preço"));
        } catch (NegocioException e) {
            aviso.erro(e.getMessage());
            return;
        }
        boolean inclusao = selecionado == null;
        Tarefa.executar(salvar, aviso, () -> {
            if (inclusao) controlador.incluir(item);
            else controlador.alterar(item);
            return item;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Item " + item.codigo() + " gravado.");
        });
    }

    private void excluir(Button origem) {
        if (selecionado == null) {
            aviso.erro("Selecione um item na lista.");
            return;
        }
        ItemConsumo alvo = selecionado;
        if (!Campos.confirmar("Excluir o item " + alvo.codigo() + "?")) return;
        Tarefa.executar(origem, aviso, () -> {
            controlador.excluir(alvo);
            return alvo;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Item " + alvo.codigo() + " excluído.");
        });
    }
}
