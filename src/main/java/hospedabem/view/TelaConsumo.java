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

import java.math.BigDecimal;

/** UC11: localizar a hospedagem ativa e lançar itens do catálogo na conta. */
public class TelaConsumo extends Pagina {
    private final ControladorConsumo controlador;
    private final TableView<ResumoHospedagem> hospedagens = Campos.tabela(
            Campos.coluna("Hospedagem", ResumoHospedagem::codigo),
            Campos.coluna("Quarto", ResumoHospedagem::numeroQuarto),
            Campos.coluna("Hóspedes", ResumoHospedagem::hospedes),
            Campos.coluna("Entrada", r -> Campos.DATA_HORA.format(r.entrada())));
    private final TableView<Consumo> lancamentos = Campos.tabela(
            Campos.coluna("Data/hora", c -> Campos.DATA_HORA.format(c.dataHora())),
            Campos.coluna("Item", c -> c.codigoItem() + " - " + c.descricaoItem()),
            Campos.coluna("Qtd.", Consumo::quantidade),
            Campos.coluna("Unitário", c -> Campos.moeda(c.precoUnitario())),
            Campos.coluna("Total", c -> Campos.moeda(c.valorTotal())));
    private final ComboBox<ItemConsumo> item = new ComboBox<>();
    private final TextField quantidade = new TextField("1");
    private final Label conta = new Label("Selecione uma hospedagem na lista.");
    private final Button lancar = Campos.botao("Lançar", "botao-primario");
    private Integer codigoSelecionado;

    public TelaConsumo(ControladorConsumo controlador) {
        super("Lançar consumo/serviço", "Registre itens e serviços consumidos durante uma hospedagem ativa.");
        this.controlador = controlador;
        item.setMaxWidth(Double.MAX_VALUE);
        item.setPromptText("Selecione o item do catálogo");
        lancar.setOnAction(e -> lancar());
        lancar.setDisable(true);
        hospedagens.getSelectionModel().selectedItemProperty().addListener((obs, antes, r) -> {
            if (r != null) localizar(r.codigo());
        });
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

    private void localizar(int codigo) {
        Tarefa.executar(hospedagens, aviso, () -> controlador.localizar(codigo), this::mostrar);
    }

    private void mostrar(Hospedagem h) {
        codigoSelecionado = h.getCodigo();
        BigDecimal soma = h.getConta().getConsumos().stream().map(Consumo::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        conta.setText("Hospedagem " + h.getCodigo() + " · Quarto " + h.getQuarto().getNumero() + " · conta "
                + h.getConta().getSituacao() + " · consumos: " + Campos.moeda(soma));
        lancamentos.getItems().setAll(h.getConta().getConsumos());
        lancar.setDisable(false);
    }

    private void lancar() {
        int qtd;
        ItemConsumo escolhido = item.getValue();
        try {
            if (codigoSelecionado == null) throw new NegocioException("Selecione uma hospedagem na lista.");
            if (escolhido == null) throw new NegocioException("Selecione um item do catálogo.");
            qtd = Campos.inteiro(quantidade.getText(), "Quantidade");
            Consumo.exigirQuantidade(qtd);
        } catch (NegocioException e) {
            aviso.erro(e.getMessage());
            return;
        }
        int codigo = codigoSelecionado;
        Tarefa.executar(lancar, aviso, () -> {
            Consumo c = controlador.lancar(codigo, escolhido.codigo(), qtd);
            return new Object[]{c, controlador.localizar(codigo)};
        }, r -> {
            Consumo c = (Consumo) r[0];
            mostrar((Hospedagem) r[1]);
            quantidade.setText("1");
            aviso.sucesso("Lançado: " + c.quantidade() + " × " + c.descricaoItem() + " = " + Campos.moeda(c.valorTotal()) + ".");
        });
    }
}
