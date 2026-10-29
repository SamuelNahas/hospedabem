package hospedabem.view;

import hospedabem.controller.ControladorCheckIn;
import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.model.Atendimento;
import hospedabem.model.Hospedagem;
import hospedabem.model.Hospede;
import hospedabem.model.Quarto;
import hospedabem.model.Reserva;
import hospedabem.model.TipoQuarto;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.LocalDate;

public class TelaCheckIn extends Pagina {
    private final ControladorCheckIn controlador;
    private final Label dadosAtendimento = new Label();
    private final ListView<String> participantes = new ListView<>();
    private final TableView<Quarto> quartos = Campos.tabela(
            Campos.coluna("Número", Quarto::getNumero),
            Campos.coluna("Andar", Quarto::getAndar),
            Campos.coluna("Tipo", Quarto::getTipo),
            Campos.coluna("Capacidade", q -> q.getTipo().capacidadeMaxima()),
            Campos.coluna("Diária", q -> Campos.moeda(q.getTipo().valorDiaria())));
    private final Label resumo = new Label();

    public TelaCheckIn(ControladorCheckIn controlador, ControladorDisponibilidade consultas) {
        super("Check-in", "Registra a entrada dos hóspedes, ocupa o quarto e abre a conta da hospedagem.");
        this.controlador = controlador;
        dadosAtendimento.getStyleClass().add("destaque");
        dadosAtendimento.setWrapText(true);
        resumo.getStyleClass().add("resumo");
        resumo.setWrapText(true);
        resumo.setMaxWidth(Double.MAX_VALUE);
        participantes.setPrefHeight(110);
        quartos.setPrefHeight(170);
        getChildren().addAll(etapaInicio(consultas), etapaParticipantes(), etapaQuarto(), etapaConfirmacao());
    }

    private Node etapaInicio(ControladorDisponibilidade consultas) {
        ToggleGroup modo = new ToggleGroup();
        ToggleButton comReserva = new ToggleButton("Com reserva");
        ToggleButton semReserva = new ToggleButton("Sem reserva");
        comReserva.setToggleGroup(modo);
        semReserva.setToggleGroup(modo);
        comReserva.setSelected(true);
        comReserva.getStyleClass().add("segmento");
        semReserva.getStyleClass().add("segmento");
        modo.selectedToggleProperty().addListener((obs, antes, agora) -> {
            if (agora == null) antes.setSelected(true);
        });

        TextField codigo = new TextField();
        codigo.setPromptText("ex.: 1001");
        TextField cpf = new TextField();
        cpf.setPromptText("CPF ou passaporte");
        ComboBox<TipoQuarto> tipo = new ComboBox<>();
        tipo.setMaxWidth(Double.MAX_VALUE);
        DatePicker entrada = Campos.seletorData(LocalDate.now());
        DatePicker saida = Campos.seletorData(LocalDate.now().plusDays(1));
        GridPane formComReserva = Campos.formulario("Código da reserva", codigo);
        GridPane formSemReserva = Campos.formulario("CPF/passaporte do responsável", cpf, "Tipo de quarto", tipo,
                "Entrada (hoje)", entrada, "Saída prevista", saida);
        formSemReserva.visibleProperty().bind(semReserva.selectedProperty());
        formSemReserva.managedProperty().bind(semReserva.selectedProperty());
        formComReserva.visibleProperty().bind(comReserva.selectedProperty());
        formComReserva.managedProperty().bind(comReserva.selectedProperty());

        Button iniciar = Campos.botao("Iniciar check-in", "botao-primario");
        iniciar.setOnAction(e -> {
            limparEtapas();
            if (comReserva.isSelected()) {
                String c = codigo.getText();
                Tarefa.executar(iniciar, aviso, () -> controlador.iniciarCheckIn(Campos.inteiro(c, "Código da reserva")),
                        r -> dadosAtendimento.setText(descrever(r)));
            } else {
                String doc = cpf.getText();
                String de = Campos.textoData(entrada);
                String ate = Campos.textoData(saida);
                String codTipo = tipo.getValue() == null ? null : tipo.getValue().codigo();
                Tarefa.executar(iniciar, aviso, () -> controlador.iniciarCheckInSemReserva(doc, codTipo,
                                Campos.data(de, "Data de entrada"), Campos.data(ate, "Data de saída")),
                        a -> dadosAtendimento.setText(descreverSemReserva(a)));
            }
        });
        Tarefa.executar(iniciar, aviso, consultas::listarTipos, lista -> {
            tipo.getItems().setAll(lista);
            if (!lista.isEmpty()) tipo.setValue(lista.get(0));
        });
        HBox segmentos = new HBox(comReserva, semReserva);
        segmentos.getStyleClass().add("segmentos");
        return Campos.cartao("1 · Reserva ou atendimento sem reserva", segmentos, formComReserva, formSemReserva,
                Campos.acoes(iniciar), dadosAtendimento);
    }

    private Node etapaParticipantes() {
        TextField documento = new TextField();
        documento.setPromptText("CPF ou passaporte do hóspede");
        Button adicionar = Campos.botao("Adicionar hóspede", "botao-primario");
        Button remover = Campos.botao("Remover selecionado", "botao-secundario");
        adicionar.setOnAction(e -> {
            String doc = documento.getText();
            Tarefa.executar(adicionar, aviso, () -> controlador.adicionarHospede(doc), h -> {
                documento.clear();
                atualizarParticipantes();
            });
        });
        remover.setOnAction(e -> {
            int i = participantes.getSelectionModel().getSelectedIndex();
            if (i >= 0) controlador.removerHospede(controlador.getAtendimento().getHospedes().get(i));
            atualizarParticipantes();
        });
        HBox linha = new HBox(documento, adicionar, remover);
        linha.getStyleClass().add("linha");
        HBox.setHgrow(documento, Priority.ALWAYS);
        return Campos.cartao("2 · Hóspedes da estadia (inclua o responsável)", linha, participantes);
    }

    private Node etapaQuarto() {
        Button consultar = Campos.botao("Consultar quartos disponíveis", "botao-primario");
        Button selecionar = Campos.botao("Selecionar quarto", "botao-secundario");
        consultar.setOnAction(e -> Tarefa.executar(consultar, aviso, controlador::consultarQuartosDisponiveis,
                lista -> quartos.getItems().setAll(lista)));
        selecionar.setOnAction(e -> {
            Quarto q = quartos.getSelectionModel().getSelectedItem();
            if (q == null) {
                aviso.erro("Selecione um quarto na lista.");
                return;
            }
            Tarefa.executar(selecionar, aviso, () -> controlador.selecionarQuarto(q.getNumero()),
                    r -> resumo.setText(descrever(r)));
        });
        return Campos.cartao("3 · Quarto", Campos.acoes(consultar, selecionar), quartos);
    }

    private Node etapaConfirmacao() {
        Button confirmar = Campos.botao("Confirmar check-in", "botao-primario");
        confirmar.setOnAction(e -> Tarefa.executar(confirmar, aviso, controlador::confirmarCheckIn, h -> {
            limparEtapas();
            dadosAtendimento.setText("");
            resumo.setText(descrever(h));
            aviso.sucesso("Check-in realizado com sucesso. Hospedagem " + h.getCodigo() + " · quarto "
                    + h.getQuarto().getNumero() + " ocupado · conta nº " + h.getConta().getId() + " aberta.");
        }));
        return Campos.cartao("4 · Resumo e confirmação", resumo, Campos.acoes(confirmar));
    }

    private void atualizarParticipantes() {
        participantes.getItems().setAll(controlador.getAtendimento().getHospedes().stream()
                .map(h -> h.cpfOuPassaporte() + " · " + h.nome()).toList());
        quartos.getItems().clear();
        resumo.setText("");
    }

    private void limparEtapas() {
        participantes.getItems().clear();
        quartos.getItems().clear();
        resumo.setText("");
    }

    private static String descrever(Reserva r) {
        return "Reserva " + r.getCodigo() + " (" + r.getSituacao() + ")\nResponsável: " + r.getResponsavel().nome()
                + " · " + r.getResponsavel().cpfOuPassaporte() + "\nTipo: " + r.getTipo() + " · capacidade "
                + r.getTipo().capacidadeMaxima() + "\nPeríodo previsto: " + Campos.texto(r.getPeriodo().entrada())
                + " a " + Campos.texto(r.getPeriodo().saida()) + " · " + r.getQuantidadeHospedes() + " hóspede(s)";
    }

    private static String descreverSemReserva(Atendimento a) {
        TipoQuarto t = a.getTipo();
        Hospede h = a.getResponsavel();
        return "Sem reserva\nResponsável: " + h.nome() + " · " + h.cpfOuPassaporte() + "\nTipo: " + t
                + " · capacidade " + t.capacidadeMaxima() + " · diária " + Campos.moeda(t.valorDiaria())
                + "\nPeríodo: " + Campos.texto(a.getPeriodo().entrada()) + " a " + Campos.texto(a.getPeriodo().saida());
    }

    private static String descrever(Atendimento a) {
        Quarto q = a.getQuarto();
        return (a.getReserva() == null ? "Check-in sem reserva" : "Reserva " + a.getReserva().getCodigo())
                + "\nResponsável: " + a.getResponsavel().nome()
                + "\nHóspedes: " + String.join("; ", a.getHospedes().stream().map(Hospede::nome).toList())
                + "\nQuarto: " + q.getNumero() + " (andar " + q.getAndar() + ", " + q.getTipo() + ")"
                + "\nPeríodo previsto: " + Campos.texto(a.getPeriodo().entrada()) + " a " + Campos.texto(a.getPeriodo().saida())
                + "\nDiária: " + Campos.moeda(q.getTipo().valorDiaria());
    }

    private static String descrever(Hospedagem h) {
        return "Hospedagem " + h.getCodigo() + "\nEntrada: " + Campos.DATA_HORA.format(h.getDataHoraEntrada())
                + "\nSaída prevista: " + Campos.texto(h.getDataSaidaPrevista())
                + "\nQuarto ocupado: " + h.getQuarto().getNumero()
                + "\nConta aberta: nº " + h.getConta().getId() + " (" + h.getConta().getSituacao() + ")";
    }
}
