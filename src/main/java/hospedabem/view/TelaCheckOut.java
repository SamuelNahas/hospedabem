package hospedabem.view;

import hospedabem.controller.ControladorCheckOut;
import hospedabem.model.Consumo;
import hospedabem.model.Extrato;
import hospedabem.model.FormaPagamento;
import hospedabem.model.ResumoHospedagem;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/**
 * UC12 em três passos na mesma página (RNF-12: até cinco telas): 1) localizar a hospedagem,
 * 2) conferir diárias, consumos e total, 3) escolher a forma e confirmar o pagamento. Até a
 * confirmação nada é gravado: a hospedagem continua ativa e o quarto ocupado.
 */
public class TelaCheckOut extends Pagina {
    private final ControladorCheckOut controlador;
    private final TableView<ResumoHospedagem> hospedagens = Campos.tabela(
            Campos.coluna("Hospedagem", ResumoHospedagem::codigo),
            Campos.coluna("Quarto", ResumoHospedagem::numeroQuarto),
            Campos.coluna("Hóspedes", ResumoHospedagem::hospedes),
            Campos.coluna("Entrada", r -> Campos.DATA_HORA.format(r.entrada())),
            Campos.coluna("Saída prevista", r -> Campos.texto(r.saidaPrevista())));
    private final TableView<Consumo> consumos = Campos.tabela(
            Campos.coluna("Data/hora", c -> Campos.DATA_HORA.format(c.dataHora())),
            Campos.coluna("Item", c -> c.codigoItem() + " - " + c.descricaoItem()),
            Campos.coluna("Qtd.", Consumo::quantidade),
            Campos.coluna("Unitário", c -> Campos.moeda(c.precoUnitario())),
            Campos.coluna("Total", c -> Campos.moeda(c.valorTotal())));
    private final Label diarias = new Label("Selecione uma hospedagem e clique em Conferir conta.");
    private final Label subtotalConsumos = new Label();
    private final Label total = new Label();
    private final ComboBox<FormaPagamento> forma = new ComboBox<>();
    private final Button conferir = Campos.botao("Conferir conta", "botao-secundario");
    private final Button confirmar = Campos.botao("Confirmar pagamento e check-out", "botao-primario");
    private Integer codigo;
    private Extrato extrato;

    public TelaCheckOut(ControladorCheckOut controlador) {
        super("Check-out", "Confira diárias e consumos, registre o pagamento e libere o quarto.");
        this.controlador = controlador;
        total.getStyleClass().add("titulo-cartao");
        forma.getItems().setAll(FormaPagamento.values());
        forma.setPromptText("Forma de pagamento");
        forma.setMaxWidth(Double.MAX_VALUE);
        confirmar.setDisable(true);
        hospedagens.getSelectionModel().selectedItemProperty().addListener((obs, antes, r) -> limparExtrato());
        conferir.setOnAction(e -> conferir());
        confirmar.setOnAction(e -> confirmar());
        getChildren().addAll(
                Campos.cartao("1. Hospedagem ativa", hospedagens, Campos.acoes(conferir)),
                Campos.cartao("2. Conferência da conta", diarias, consumos, subtotalConsumos, total),
                Campos.cartao("3. Pagamento", Campos.formulario("Forma", forma), Campos.acoes(confirmar)));
        recarregar();
    }

    private void recarregar() {
        Tarefa.executar(conferir, aviso, controlador::listarHospedagensAtivas, l -> hospedagens.getItems().setAll(l));
    }

    private void limparExtrato() {
        codigo = null;
        extrato = null;
        consumos.getItems().clear();
        diarias.setText("Clique em Conferir conta.");
        subtotalConsumos.setText("");
        total.setText("");
        confirmar.setDisable(true);
    }

    private void conferir() {
        ResumoHospedagem r = hospedagens.getSelectionModel().getSelectedItem();
        if (r == null) {
            aviso.erro("Selecione uma hospedagem na lista.");
            return;
        }
        Tarefa.executar(conferir, aviso, () -> controlador.conferir(r.codigo()), e -> {
            codigo = r.codigo();
            extrato = e;
            diarias.setText("Diárias: " + e.quantidadeDiarias() + " × " + Campos.moeda(e.valorDiaria()) + " = "
                    + Campos.moeda(e.valorDiarias()));
            consumos.getItems().setAll(e.consumos());
            subtotalConsumos.setText("Consumos e serviços: " + Campos.moeda(e.valorConsumos()));
            total.setText("Total a pagar: " + Campos.moeda(e.total()));
            confirmar.setDisable(false);
        });
    }

    private void confirmar() {
        if (codigo == null || extrato == null) {
            aviso.erro("Confira a conta antes de confirmar.");
            return;
        }
        FormaPagamento f = forma.getValue();
        if (f == null) {
            aviso.erro("Selecione a forma de pagamento.");
            return;
        }
        int cod = codigo;
        Extrato conferido = extrato;
        if (!Campos.confirmar("Confirmar o recebimento de " + Campos.moeda(conferido.total()) + " em "
                + f.descricao() + " e concluir o check-out da hospedagem " + cod + "?")) return;
        Tarefa.executar(confirmar, aviso, () -> controlador.confirmarPagamento(cod, f, conferido.total()), p -> {
            limparExtrato();
            forma.setValue(null);
            recarregar();
            aviso.sucesso("Check-out da hospedagem " + cod + " concluído: " + Campos.moeda(p.extrato().total())
                    + " pagos em " + p.forma().descricao() + ". Quarto liberado.");
        });
    }
}
