package hospedabem.view;

import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.model.Disponibilidade;
import hospedabem.model.TipoQuarto;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.time.LocalDate;

public class TelaDisponibilidade extends Pagina {
    static final String TODOS = "Todos os tipos";

    public TelaDisponibilidade(ControladorDisponibilidade controlador) {
        super("Disponibilidade", "Quartos livres por tipo no período. O dia da saída não ocupa diária.");
        DatePicker entrada = Campos.seletorData(LocalDate.now());
        DatePicker saida = Campos.seletorData(LocalDate.now().plusDays(1));
        ComboBox<Object> tipo = new ComboBox<>();
        tipo.getItems().add(TODOS);
        tipo.setValue(TODOS);
        tipo.setMaxWidth(Double.MAX_VALUE);
        TableView<Disponibilidade> tabela = Campos.tabela(
                Campos.coluna("Tipo", Disponibilidade::tipo),
                Campos.coluna("Capacidade", d -> d.tipo().capacidadeMaxima()),
                Campos.coluna("Diária", d -> Campos.moeda(d.tipo().valorDiaria())),
                Campos.coluna("Quartos operacionais", Disponibilidade::quartosOperacionais),
                Campos.coluna("Ocupação máxima", Disponibilidade::picoOcupacao),
                Campos.coluna("Disponíveis", Disponibilidade::livres));
        Button consultar = Campos.botao("Consultar", "botao-primario");

        consultar.setOnAction(e -> {
            String de = Campos.textoData(entrada);
            String ate = Campos.textoData(saida);
            String codigo = tipo.getValue() instanceof TipoQuarto t ? t.codigo() : null;
            Tarefa.executar(consultar, aviso, () -> controlador.consultar(Campos.data(de, "Data de entrada"),
                    Campos.data(ate, "Data de saída"), codigo), resultado -> tabela.getItems().setAll(resultado));
        });

        Label legenda = new Label("Ocupação máxima: maior número simultâneo de reservas confirmadas e hospedagens "
                + "ativas em algum dia do período. Quartos em manutenção não contam.");
        legenda.getStyleClass().add("dica");
        legenda.setWrapText(true);
        getChildren().addAll(
                Campos.cartao("Período", Campos.formulario("Entrada", entrada, "Saída", saida, "Tipo", tipo),
                        Campos.acoes(consultar)),
                Campos.cartao("Resultado", tabela, legenda));
        Tarefa.executar(consultar, aviso, controlador::listarTipos, lista -> tipo.getItems().addAll(lista));
    }
}
