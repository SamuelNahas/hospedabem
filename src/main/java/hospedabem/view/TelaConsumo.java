package hospedabem.view;

import hospedabem.controller.ControladorConsumo;
import hospedabem.model.Consumo;
import hospedabem.model.Hospedagem;
import hospedabem.model.ItemConsumo;
import hospedabem.model.NegocioException;
import hospedabem.model.ResumoHospedagem;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** UC11: localizar a hospedagem ativa e lançar itens do catálogo na conta. */
public class TelaConsumo extends Pagina {
    private final ControladorConsumo controlador;
    private final TableView<ResumoHospedagem> hospedagens = Campos.tabelaDeHospedagens();
    private final TableView<Consumo> lancamentos = Campos.tabelaDeConsumos();
    private final ComboBox<ItemConsumo> item = new ComboBox<>();
    private final TextField quantidade = new TextField("1");
    private final Label conta = new Label("Selecione uma hospedagem na lista.");
    private final Button lancar = Campos.botao("Lançar", "botao-primario");
    private Integer codigoSelecionado;
    private String cabecalhoDaConta = "";

    public TelaConsumo(ControladorConsumo controlador) {
        super("Lançar consumo/serviço", "Registre itens e serviços consumidos durante uma hospedagem ativa.");
        this.controlador = controlador;
        item.setMaxWidth(Double.MAX_VALUE);
        item.setPromptText("Selecione o item do catálogo");
        lancar.setOnAction(e -> lancar());
        lancar.setDisable(true);
        hospedagens.getSelectionModel().selectedItemProperty().addListener((obs, antes, r) -> selecionar(r));
        getChildren().addAll(
                Campos.cartao("Hospedagens ativas", hospedagens),
                Campos.cartao("Conta da hospedagem", conta, lancamentos),
                Campos.cartao("Novo lançamento",
                        Campos.formulario("Item", item, "Quantidade", quantidade),
                        Campos.acoes(lancar)));
        Tarefa.executar(lancar, aviso, controlador::listarHospedagensAtivas, l -> {
            hospedagens.getItems().setAll(l);
            lancar.setDisable(true);
        });
        Tarefa.executar(item, aviso, controlador::listarItens, l -> item.getItems().setAll(l));
    }

    /** Nada pode ser lançado até a conta da hospedagem selecionada terminar de carregar. */
    private void selecionar(ResumoHospedagem r) {
        codigoSelecionado = null;
        lancar.setDisable(true);
        lancamentos.getItems().clear();
        if (r == null) {
            conta.setText("Selecione uma hospedagem na lista.");
            return;
        }
        conta.setText("Carregando a conta da hospedagem " + r.codigo() + "...");
        Tarefa.executar(hospedagens, aviso, () -> controlador.localizar(r.codigo()), h -> {
            if (hospedagens.getSelectionModel().getSelectedItem() == r) mostrar(h);
        });
    }

    private void mostrar(Hospedagem h) {
        codigoSelecionado = h.getCodigo();
        cabecalhoDaConta = "Hospedagem " + h.getCodigo() + " · Quarto " + h.getQuarto().getNumero() + " · conta "
                + h.getConta().getSituacao();
        lancamentos.getItems().setAll(h.getConta().getConsumos());
        atualizarTotal();
        lancar.setDisable(false);
    }

    private void atualizarTotal() {
        conta.setText(cabecalhoDaConta + " · consumos: " + Campos.moeda(Consumo.somar(lancamentos.getItems())));
    }

    /**
     * Só o lançamento roda na tarefa: o consumo gravado é acrescentado à lista da tela, sem reler a
     * conta, para uma falha posterior não ser confundida com falha do lançamento.
     */
    private void lancar() {
        int qtd;
        ItemConsumo escolhido = item.getValue();
        try {
            if (codigoSelecionado == null) throw new NegocioException("Selecione uma hospedagem na lista.");
            if (escolhido == null) throw new NegocioException("Selecione um item do catálogo.");
            qtd = Campos.inteiro(quantidade.getText(), "Quantidade");
        } catch (NegocioException e) {
            aviso.erro(e.getMessage());
            return;
        }
        int codigo = codigoSelecionado;
        Tarefa.executar(lancar, aviso, () -> controlador.lancar(codigo, escolhido.codigo(), qtd), c -> {
            if (Integer.valueOf(codigo).equals(codigoSelecionado)) {
                lancamentos.getItems().add(c);
                atualizarTotal();
            }
            quantidade.setText("1");
            aviso.sucesso("Lançado: " + c.quantidade() + " × " + c.descricaoItem() + " = " + Campos.moeda(c.valorTotal()) + ".");
        });
    }
}
