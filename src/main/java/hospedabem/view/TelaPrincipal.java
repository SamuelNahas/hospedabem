package hospedabem.view;

import hospedabem.controller.ControladorAutenticacao;
import hospedabem.controller.ControladorCheckIn;
import hospedabem.controller.ControladorCheckOut;
import hospedabem.controller.ControladorConsumo;
import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.controller.ControladorFuncionario;
import hospedabem.controller.ControladorHospede;
import hospedabem.controller.ControladorQuarto;
import hospedabem.controller.ControladorReserva;
import hospedabem.controller.ControladorTipoQuarto;
import hospedabem.controller.ControladorItemConsumo;
import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TelaPrincipal extends BorderPane {
    private final ScrollPane conteudo = new ScrollPane();
    private final VBox area = new VBox();
    private final List<Button> navegacao = new ArrayList<>();

    /** Monta a navegação da sessão e desabilita os cadastros administrativos para recepcionistas. */
    public TelaPrincipal(ConfigurableApplicationContext ctx, Funcionario funcionario) {
        boolean admin = funcionario.perfil() == Perfil.ADMINISTRADOR;
        Label marca = new Label("HospedaBem");
        marca.getStyleClass().add("marca");

        Button disponibilidade = item("Disponibilidade", () -> new TelaDisponibilidade(ctx.getBean(ControladorDisponibilidade.class)));
        Button reserva = item("Registrar reserva", () -> new TelaReserva(ctx.getBean(ControladorReserva.class),
                ctx.getBean(ControladorDisponibilidade.class)));
        Button checkIn = item("Check-in", () -> new TelaCheckIn(ctx.getBean(ControladorCheckIn.class),
                ctx.getBean(ControladorDisponibilidade.class)));
        Button consumo = item("Lançar consumo", () -> new TelaConsumo(ctx.getBean(ControladorConsumo.class)));
        Button checkOut = item("Check-out", () -> new TelaCheckOut(ctx.getBean(ControladorCheckOut.class)));
        Button trocarSenha = item("Trocar senha", () -> TelaTrocaSenha.voluntaria(ctx.getBean(ControladorAutenticacao.class)));
        Button tipos = item("Tipos de quarto", () -> new TelaTipoQuarto(ctx.getBean(ControladorTipoQuarto.class)));
        Button quartos = item("Quartos", () -> new TelaQuarto(ctx.getBean(ControladorQuarto.class),
                ctx.getBean(ControladorDisponibilidade.class)));
        Button hospedes = item("Hóspedes", () -> new TelaHospede(ctx.getBean(ControladorHospede.class)));
        Button funcionarios = item("Funcionários", () -> new TelaFuncionario(ctx.getBean(ControladorFuncionario.class)));
        Button itens = item("Itens de consumo e serviços", () -> new TelaItemConsumo(ctx.getBean(ControladorItemConsumo.class)));
        if (!admin) {
            for (Button b : List.of(tipos, quartos, itens, funcionarios)) {
                b.setDisable(true);
                b.setTooltip(new Tooltip("Restrito ao administrador (RNF-10)"));
            }
        }

        Label secaoAtendimento = secao("ATENDIMENTO");
        Label secaoCadastros = secao("CADASTROS");
        Region espaco = new Region();
        VBox.setVgrow(espaco, Priority.ALWAYS);
        Label usuario = new Label(funcionario.nome() + "\n" + funcionario.matricula() + " · "
                + (admin ? "Administrador" : "Recepcionista"));
        usuario.getStyleClass().add("usuario");
        Button sair = Campos.botao("Sair", "botao-sair");
        sair.setMaxWidth(Double.MAX_VALUE);
        sair.setOnAction(e -> {
            ctx.getBean(ControladorAutenticacao.class).sair();
            getScene().setRoot(new TelaLogin(ctx));
        });

        VBox barra = new VBox(marca, secaoAtendimento, disponibilidade, reserva, checkIn, consumo,
                checkOut, secaoCadastros, hospedes, tipos,
                quartos, itens, funcionarios, espaco, usuario, trocarSenha, sair);
        barra.getStyleClass().add("barra-lateral");
        conteudo.setFitToWidth(true);
        conteudo.getStyleClass().add("conteudo");
        setLeft(barra);
        VBox.setVgrow(conteudo, Priority.ALWAYS);
        area.getStyleClass().add("conteudo");
        setCenter(area);
        checkIn.fire();
    }

    private Button item(String texto, Supplier<Pagina> pagina) {
        Button b = new Button(texto);
        b.getStyleClass().add("item-navegacao");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            navegacao.forEach(n -> n.getStyleClass().remove("ativo"));
            b.getStyleClass().add("ativo");
            Pagina p = pagina.get();
            VBox.setMargin(p.getAviso(), new Insets(16, 32, 0, 32));
            area.getChildren().setAll(p.getAviso(), conteudo);
            conteudo.setContent(p);
            conteudo.setVvalue(0);
        });
        navegacao.add(b);
        return b;
    }

    private static Label secao(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("secao-navegacao");
        return l;
    }
}
