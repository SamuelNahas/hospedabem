package hospedabem.view;

import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.controller.ControladorReserva;
import hospedabem.controller.Disponibilidade;
import hospedabem.model.Reserva;
import hospedabem.model.TipoQuarto;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.LocalDate;

/** UC08 Registrar Reserva. O código é gerado pelo banco e o funcionário é o da sessão. */
public class TelaReserva extends Pagina {

    public TelaReserva(ControladorReserva controlador, ControladorDisponibilidade consultas) {
        super("Registrar reserva", "A reserva garante um tipo de quarto no período; o quarto é escolhido no check-in.");
        TextField cpf = new TextField();
        cpf.setPromptText("CPF ou passaporte");
        Button buscar = Campos.botao("Buscar hóspede", "botao-secundario");
        Label nome = new Label("—");
        nome.getStyleClass().add("destaque");
        ComboBox<TipoQuarto> tipo = new ComboBox<>();
        tipo.setMaxWidth(Double.MAX_VALUE);
        DatePicker entrada = Campos.seletorData(LocalDate.now());
        DatePicker saida = Campos.seletorData(LocalDate.now().plusDays(1));
        TextField quantidade = new TextField("1");
        Label disponibilidade = new Label();
        disponibilidade.getStyleClass().add("dica");
        Button verificar = Campos.botao("Verificar disponibilidade", "botao-secundario");
        Button registrar = Campos.botao("Confirmar reserva", "botao-primario");

        buscar.setOnAction(e -> {
            String doc = cpf.getText();
            Tarefa.executar(buscar, aviso, () -> controlador.buscarResponsavel(doc),
                    h -> nome.setText(h.nome() + " · tel. " + h.telefone()));
        });
        verificar.setOnAction(e -> {
            String de = Campos.textoData(entrada);
            String ate = Campos.textoData(saida);
            String codigo = tipo.getValue() == null ? null : tipo.getValue().codigo();
            Tarefa.executar(verificar, aviso, () -> consultas.consultar(Campos.data(de, "Data de entrada"),
                    Campos.data(ate, "Data de saída"), codigo).get(0), d -> disponibilidade.setText(texto(d)));
        });
        registrar.setOnAction(e -> {
            String doc = cpf.getText();
            String de = Campos.textoData(entrada);
            String ate = Campos.textoData(saida);
            String qtd = quantidade.getText();
            String codigo = tipo.getValue() == null ? null : tipo.getValue().codigo();
            Tarefa.executar(registrar, aviso, () -> controlador.registrar(doc, codigo, Campos.data(de, "Data de entrada"),
                    Campos.data(ate, "Data de saída"), Campos.inteiro(qtd, "Quantidade de hóspedes")),
                    r -> aviso.sucesso(resumo(r)));
        });

        HBox linhaCpf = new HBox(cpf, buscar);
        linhaCpf.getStyleClass().add("linha");
        HBox.setHgrow(cpf, Priority.ALWAYS);
        getChildren().addAll(
                Campos.cartao("Hóspede responsável", Campos.formulario("CPF/passaporte do responsável", linhaCpf,
                        "Responsável", nome)),
                Campos.cartao("Estadia", Campos.formulario("Tipo de quarto", tipo, "Entrada prevista", entrada,
                        "Saída prevista", saida, "Quantidade de hóspedes", quantidade), disponibilidade,
                        Campos.acoes(registrar, verificar)));
        Tarefa.executar(registrar, aviso, consultas::listarTipos, lista -> {
            tipo.getItems().setAll(lista);
            if (!lista.isEmpty()) tipo.setValue(lista.get(0));
        });
    }

    private static String texto(Disponibilidade d) {
        return d.livres() > 0
                ? d.livres() + " quarto(s) " + d.tipo().codigo() + " disponível(is) · capacidade "
                + d.tipo().capacidadeMaxima() + " · diária " + Campos.moeda(d.tipo().valorDiaria())
                : "Sem disponibilidade do tipo " + d.tipo().codigo() + " no período.";
    }

    private static String resumo(Reserva r) {
        return "Reserva " + r.getCodigo() + " registrada · " + r.getResponsavel().nome() + " · " + r.getTipo()
                + " · " + Campos.texto(r.getPeriodo().entrada()) + " a " + Campos.texto(r.getPeriodo().saida())
                + " · " + r.getQuantidadeHospedes() + " hóspede(s)";
    }
}
