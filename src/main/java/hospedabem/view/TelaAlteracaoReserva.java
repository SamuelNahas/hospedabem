package hospedabem.view;

import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.controller.ControladorReserva;
import hospedabem.model.Reserva;
import hospedabem.model.SituacaoReserva;
import hospedabem.model.TipoQuarto;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * UC09 Consultar, Alterar e Cancelar Reserva. A reserva é localizada pelo código e os dados e a
 * situação aparecem antes de qualquer alteração; o cancelamento pede confirmação. A tela guarda a
 * reserva como foi localizada e a devolve ao controlador, que recusa a gravação se outra estação
 * (ou um check-in) a mudou nesse meio-tempo.
 */
public class TelaAlteracaoReserva extends Pagina {
    private final ControladorReserva controlador;
    private final TextField codigo = new TextField();
    private final TextField cpf = new TextField();
    private final ComboBox<TipoQuarto> tipo = new ComboBox<>();
    private final DatePicker entrada = Campos.seletorData(null);
    private final DatePicker saida = Campos.seletorData(null);
    private final TextField quantidade = new TextField();
    private final Label situacao = new Label("—");
    private final Button salvar = Campos.botao("Salvar alteração", "botao-primario");
    private final Button cancelar = Campos.botao("Cancelar reserva", "botao-perigo");
    private Reserva localizada;

    public TelaAlteracaoReserva(ControladorReserva controlador, ControladorDisponibilidade consultas) {
        super("Alterar ou cancelar reserva", "A recepção localiza a reserva pelo código e altera responsável, tipo de "
                + "quarto, datas previstas ou quantidade de hóspedes, ou a cancela. A reserva já convertida em "
                + "hospedagem ou já cancelada não é mais alterada; a cancelada é preservada como registro e sua "
                + "capacidade volta à consulta de disponibilidade.");
        this.controlador = controlador;
        codigo.setPromptText("Código da reserva, como 1001");
        situacao.getStyleClass().add("destaque");
        tipo.setMaxWidth(Double.MAX_VALUE);
        Button localizar = Campos.botao("Localizar reserva", "botao-secundario");
        localizar.setOnAction(e -> localizar(localizar));
        salvar.setOnAction(e -> salvar());
        cancelar.setOnAction(e -> cancelar());
        preencher(null);

        getChildren().addAll(
                Campos.cartao("Reserva", Campos.formulario("Código da reserva", codigo),
                        Campos.acoes(localizar), situacao),
                Campos.cartao("Dados da estadia",
                        Campos.formulario("CPF/passaporte do responsável", cpf, "Tipo de quarto", tipo,
                                "Entrada prevista", entrada, "Saída prevista", saida,
                                "Quantidade de hóspedes", quantidade),
                        Campos.acoes(salvar, cancelar)));
        Tarefa.executar(localizar, aviso, consultas::listarTipos, lista -> tipo.getItems().setAll(lista));
    }

    private void localizar(Button origem) {
        String texto = codigo.getText();
        preencher(null); // a busca que falhar não deixa a reserva anterior no formulário
        Tarefa.executar(origem, aviso, () -> controlador.consultar(Campos.inteiro(texto, "Código da reserva")),
                this::preencher);
    }

    /** Mostra os dados e a situação da reserva localizada; só a CONFIRMADA pode ser alterada ou cancelada. */
    private void preencher(Reserva r) {
        localizada = r;
        situacao.setText(r == null ? "—" : resumo(r));
        cpf.setText(r == null ? "" : r.getResponsavel().cpfOuPassaporte());
        tipo.setValue(r == null ? null : r.getTipo());
        entrada.getEditor().setText(r == null ? "" : Campos.texto(r.getPeriodo().entrada()));
        saida.getEditor().setText(r == null ? "" : Campos.texto(r.getPeriodo().saida()));
        quantidade.setText(r == null ? "" : String.valueOf(r.getQuantidadeHospedes()));
        boolean alteravel = r != null && r.getSituacao() == SituacaoReserva.CONFIRMADA;
        salvar.setDisable(!alteravel);
        cancelar.setDisable(!alteravel);
    }

    private void salvar() {
        Reserva alvo = localizada;
        String doc = cpf.getText();
        String de = Campos.textoData(entrada);
        String ate = Campos.textoData(saida);
        String qtd = quantidade.getText();
        String codTipo = tipo.getValue() == null ? null : tipo.getValue().codigo();
        Tarefa.executar(salvar, aviso, () -> controlador.alterar(alvo, doc, codTipo,
                Campos.data(de, "Entrada prevista"), Campos.data(ate, "Saída prevista"),
                Campos.inteiro(qtd, "Quantidade de hóspedes")), r -> {
                    preencher(r);
                    aviso.sucesso("Reserva " + r.getCodigo() + " alterada · " + resumo(r));
                });
    }

    private void cancelar() {
        Reserva alvo = localizada;
        if (!Campos.confirmar("Cancelar a reserva " + alvo.getCodigo() + " de " + alvo.getResponsavel().nome()
                + "? O registro é preservado e a capacidade volta à disponibilidade.")) return;
        Tarefa.executar(cancelar, aviso, () -> controlador.cancelar(alvo), r -> {
            preencher(r);
            aviso.sucesso("Reserva " + r.getCodigo() + " cancelada. A capacidade do tipo "
                    + r.getTipo().codigo() + " voltou à consulta de disponibilidade.");
        });
    }

    private static String resumo(Reserva r) {
        return "Reserva " + r.getCodigo() + " · " + r.getSituacao() + " · " + r.getResponsavel().nome()
                + " · " + r.getTipo() + " · " + Campos.texto(r.getPeriodo().entrada()) + " a "
                + Campos.texto(r.getPeriodo().saida()) + " · " + r.getQuantidadeHospedes() + " hóspede(s)";
    }
}
