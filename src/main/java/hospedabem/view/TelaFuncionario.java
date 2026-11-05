package hospedabem.view;

import hospedabem.controller.ControladorFuncionario;
import hospedabem.model.Funcionario;
import hospedabem.model.NegocioException;
import hospedabem.model.Perfil;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.Arrays;

public class TelaFuncionario extends Pagina {
    private final ControladorFuncionario controlador;
    private final TableView<Funcionario> tabela = Campos.tabela(
            Campos.coluna("Matrícula", Funcionario::matricula),
            Campos.coluna("Nome", Funcionario::nome),
            Campos.coluna("CPF", Funcionario::cpf),
            Campos.coluna("Perfil", f -> f.ehAdministrador() ? "Administrador" : "Recepcionista"),
            Campos.coluna("Contratação", f -> Campos.texto(f.dataContratacao())),
            Campos.coluna("Telefone", Funcionario::telefone));
    private final TextField matricula = new TextField();
    private final TextField cpf = new TextField();
    private final TextField nome = new TextField();
    private final TextField endereco = new TextField();
    private final TextField cidade = new TextField();
    private final TextField estado = new TextField();
    private final TextField telefone = new TextField();
    private final TextField email = new TextField();
    private final DatePicker contratacao = Campos.seletorData(LocalDate.now());
    private final ComboBox<Perfil> perfil = new ComboBox<>();
    private final PasswordField senha = new PasswordField();
    private final TextField termo = new TextField();
    private final Button salvar = Campos.botao("Salvar", "botao-primario");
    private Funcionario selecionado;

    public TelaFuncionario(ControladorFuncionario controlador) {
        super("Funcionários", "O administrador cadastra funcionários, define o perfil de acesso e a senha inicial. "
                + "Só o hash da senha é gravado.");
        this.controlador = controlador;
        perfil.getItems().setAll(Perfil.values());
        perfil.setValue(Perfil.RECEPCIONISTA);
        perfil.setMaxWidth(Double.MAX_VALUE);
        termo.setPromptText("Matrícula, nome ou CPF");

        Button limpar = Campos.botao("Limpar formulário", "botao-secundario");
        Button excluir = Campos.botao("Excluir", "botao-perigo");
        Button buscar = Campos.botao("Buscar funcionário", "botao-secundario");
        Button redefinir = Campos.botao("Redefinir senha", "botao-secundario");
        limpar.setOnAction(e -> selecionar(null));
        salvar.setOnAction(e -> salvar());
        excluir.setOnAction(e -> excluir(excluir));
        redefinir.setOnAction(e -> redefinirSenha(redefinir));
        buscar.setOnAction(e -> {
            String t = termo.getText();
            Tarefa.executar(buscar, aviso, () -> controlador.procurar(t), encontrados -> {
                tabela.getItems().setAll(encontrados);
                if (encontrados.isEmpty()) aviso.erro("Nenhum funcionário para \"" + t + "\".");
            });
        });
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, f) -> {
            if (f != null) selecionar(f);
        });

        getChildren().addAll(
                Campos.cartao("Funcionários cadastrados", tabela, Campos.acoes(termo, buscar)),
                Campos.cartao("Dados do funcionário",
                        Campos.formulario("Matrícula", matricula, "CPF", cpf, "Nome", nome, "Endereço", endereco,
                                "Cidade", cidade, "Estado (UF)", estado, "Telefone", telefone, "E-mail", email,
                                "Data de contratação", contratacao, "Perfil", perfil, "Senha", senha),
                        Campos.acoes(salvar, limpar, redefinir, excluir)));
        recarregar();
    }

    private void recarregar() {
        Tarefa.executar(salvar, aviso, controlador::listar, lista -> tabela.getItems().setAll(lista));
    }

    private void selecionar(Funcionario f) {
        selecionado = f;
        matricula.setText(f == null ? "" : f.matricula());
        cpf.setText(f == null ? "" : f.cpf());
        nome.setText(f == null ? "" : f.nome());
        endereco.setText(f == null || f.endereco() == null ? "" : f.endereco());
        cidade.setText(f == null || f.cidade() == null ? "" : f.cidade());
        estado.setText(f == null || f.estado() == null ? "" : f.estado());
        telefone.setText(f == null || f.telefone() == null ? "" : f.telefone());
        email.setText(f == null || f.email() == null ? "" : f.email());
        contratacao.getEditor().setText(f == null ? Campos.texto(LocalDate.now()) : Campos.texto(f.dataContratacao()));
        perfil.setValue(f == null ? Perfil.RECEPCIONISTA : f.perfil());
        senha.clear();
        if (f == null) tabela.getSelectionModel().clearSelection();
    }

    /** Monta o funcionário com os dados digitados; em caso de recusa o formulário continua preenchido. */
    private Funcionario doFormulario() {
        return Funcionario.informado(selecionado == null ? 0 : selecionado.id(), matricula.getText(), cpf.getText(),
                nome.getText(), endereco.getText(), cidade.getText(), estado.getText(), telefone.getText(),
                email.getText(), Campos.data(Campos.textoData(contratacao), "Data de contratação"),
                perfil.getValue());
    }

    private void salvar() {
        boolean inclusao = selecionado == null;
        Funcionario f;
        char[] nova;
        try {
            f = doFormulario();
            // Salvar não troca senha: dizer isso é melhor do que gravar o resto e deixar a senha
            // antiga valendo sem avisar.
            if (!inclusao && !senha.getText().isEmpty())
                throw new NegocioException("Salvar não troca a senha. Para definir uma senha nova para "
                        + f.matricula() + ", use \"Redefinir senha\".");
            nova = senha.getText().toCharArray();
        } catch (NegocioException e) {
            aviso.erro(e.getMessage());
            return;
        }
        Tarefa.executar(salvar, aviso, () -> {
            try {
                if (inclusao) controlador.incluir(f, nova);
                else controlador.alterar(f);
            } finally {
                Arrays.fill(nova, '\0');
            }
            return f;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Funcionário " + f.matricula() + " gravado como "
                    + (f.ehAdministrador() ? "administrador" : "recepcionista") + ".");
        });
    }

    private void redefinirSenha(Button origem) {
        if (selecionado == null) {
            aviso.erro("Selecione um funcionário na lista.");
            return;
        }
        Funcionario alvo = selecionado;
        char[] nova = senha.getText().toCharArray();
        Tarefa.executar(origem, aviso, () -> {
            try {
                controlador.redefinirSenha(alvo, nova);
            } finally {
                Arrays.fill(nova, '\0');
            }
            return alvo;
        }, ok -> {
            senha.clear();
            aviso.sucesso("Senha de " + alvo.matricula() + " redefinida.");
        });
    }

    private void excluir(Button origem) {
        if (selecionado == null) {
            aviso.erro("Selecione um funcionário na lista.");
            return;
        }
        Funcionario alvo = selecionado;
        if (!Campos.confirmar("Excluir o funcionário " + alvo.matricula() + "?")) return;
        Tarefa.executar(origem, aviso, () -> {
            controlador.excluir(alvo);
            return alvo;
        }, ok -> {
            selecionar(null);
            recarregar();
            aviso.sucesso("Funcionário " + alvo.matricula() + " excluído.");
        });
    }
}
