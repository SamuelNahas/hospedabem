package hospedabem.view;

import hospedabem.controller.ControladorHospede;
import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** UC06 Gerenciar Hóspede: inclusão, alteração, listagem, busca e exclusão pela recepção. */
public class TelaHospede extends Pagina {
    private final ControladorHospede controlador;
    private final TableView<Hospede> tabela = Campos.tabela(
            Campos.coluna("CPF/Passaporte", Hospede::cpfOuPassaporte),
            Campos.coluna("Identificação", h -> h.ehCpf() ? "CPF" : "Passaporte"),
            Campos.coluna("Nome", Hospede::nome),
            Campos.coluna("Nascimento", h -> Campos.texto(h.dataNascimento())),
            Campos.coluna("Telefone", Hospede::telefone),
            Campos.coluna("Cidade", Hospede::cidade));
    private final TextField documento = new TextField();
    private final TextField nome = new TextField();
    private final DatePicker nascimento = Campos.seletorData(null);
    private final TextField telefone = new TextField();
    private final TextField endereco = new TextField();
    private final TextField cidade = new TextField();
    private final TextField estado = new TextField();
    private final TextField email = new TextField();
    private final TextField termo = new TextField();
    private final Button salvar = Campos.botao("Salvar", "botao-primario");
    private Hospede selecionado;

    public TelaHospede(ControladorHospede controlador) {
        super("Hóspedes", "A recepção cadastra o hóspede com CPF (brasileiro) ou passaporte (estrangeiro) e o usa na "
                + "reserva e no check-in. Endereço, cidade, estado e e-mail são opcionais. O hóspede com reserva ou "
                + "hospedagem não pode ser excluído: o histórico é preservado.");
        this.controlador = controlador;
        documento.setPromptText("CPF ou passaporte, como 529.982.247-25 ou XP1234567");
        termo.setPromptText("CPF, passaporte ou nome");

        Button limpar = Campos.botao("Limpar formulário", "botao-secundario");
        Button excluir = Campos.botao("Excluir", "botao-perigo");
        Button buscar = Campos.botao("Buscar hóspede", "botao-secundario");
        limpar.setOnAction(e -> selecionar(null));
        salvar.setOnAction(e -> salvar());
        excluir.setOnAction(e -> excluir(excluir));
        buscar.setOnAction(e -> {
            String t = termo.getText();
            Tarefa.executar(buscar, aviso, () -> controlador.procurar(t), encontrados -> {
                tabela.getItems().setAll(encontrados);
                if (encontrados.isEmpty()) aviso.erro("Nenhum hóspede para \"" + t + "\".");
            });
        });
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, h) -> {
            if (h != null) selecionar(h);
        });

        getChildren().addAll(
                Campos.cartao("Hóspedes cadastrados", tabela, Campos.acoes(termo, buscar)),
                Campos.cartao("Dados do hóspede",
                        Campos.formulario("CPF/Passaporte", documento, "Nome", nome,
                                "Data de nascimento", nascimento, "Telefone", telefone, "Endereço", endereco,
                                "Cidade", cidade, "Estado (UF)", estado, "E-mail", email),
                        Campos.acoes(salvar, limpar, excluir)));
        recarregar();
    }

    private void recarregar() {
        Tarefa.executar(salvar, aviso, controlador::listar, lista -> tabela.getItems().setAll(lista));
    }

    private void selecionar(Hospede h) {
        selecionado = h;
        documento.setText(h == null ? "" : h.cpfOuPassaporte());
        nome.setText(h == null ? "" : h.nome());
        nascimento.getEditor().setText(h == null ? "" : Campos.texto(h.dataNascimento()));
        telefone.setText(h == null ? "" : h.telefone());
        endereco.setText(h == null || h.endereco() == null ? "" : h.endereco());
        cidade.setText(h == null || h.cidade() == null ? "" : h.cidade());
        estado.setText(h == null || h.estado() == null ? "" : h.estado());
        email.setText(h == null || h.email() == null ? "" : h.email());
        if (h == null) tabela.getSelectionModel().clearSelection();
    }

    /** Monta o hóspede com os dados digitados; em caso de recusa o formulário continua preenchido. */
    private Hospede doFormulario() {
        return Hospede.informado(selecionado == null ? 0 : selecionado.id(), documento.getText(), nome.getText(),
                Campos.data(Campos.textoData(nascimento), "Data de nascimento"), endereco.getText(), cidade.getText(),
                estado.getText(), telefone.getText(), email.getText());
    }

    private void salvar() {
        boolean inclusao = selecionado == null;
        Hospede h;
        try {
            h = doFormulario();
        } catch (NegocioException e) {
            aviso.erro(e.getMessage());
            return;
        }
        Tarefa.executar(salvar, aviso, () -> {
            if (inclusao) controlador.incluir(h);
            else controlador.alterar(h);
            return h;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Hóspede " + h.nome() + " (" + h.cpfOuPassaporte() + ") gravado. "
                    + "Já pode ser usado na reserva e no check-in.");
        });
    }

    private void excluir(Button origem) {
        if (selecionado == null) {
            aviso.erro("Selecione um hóspede na lista.");
            return;
        }
        Hospede alvo = selecionado;
        if (!Campos.confirmar("Excluir o hóspede " + alvo.nome() + " (" + alvo.cpfOuPassaporte() + ")?")) return;
        Tarefa.executar(origem, aviso, () -> {
            controlador.excluir(alvo);
            return alvo;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Hóspede " + alvo.nome() + " excluído.");
        });
    }
}
