package hospedabem.view;

import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.controller.Disponibilidade;
import hospedabem.model.TipoQuarto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;

/** UC07 Consultar Disponibilidade de Quartos. Estadia [entrada, saída): o dia da saída não é ocupado. */
public class TelaDisponibilidade extends JDialog {
    static final String TODOS = "Todos os tipos";

    public TelaDisponibilidade(JFrame dono, ControladorDisponibilidade controlador) {
        super(dono, "Consultar disponibilidade", true);
        JTextField entrada = new JTextField(Campos.texto(LocalDate.now()), 10);
        JTextField saida = new JTextField(Campos.texto(LocalDate.now().plusDays(1)), 10);
        JComboBox<Object> tipo = new JComboBox<>(new Object[]{TODOS});
        DefaultTableModel modelo = Campos.modeloSomenteLeitura("Tipo", "Capacidade", "Diária", "Quartos operacionais",
                "Ocupação máxima no período", "Disponíveis");
        JButton consultar = new JButton("Consultar");

        consultar.addActionListener(e -> {
            String codigo = tipo.getSelectedItem() instanceof TipoQuarto t ? t.codigo() : null;
            Tarefa.executar(consultar, () -> controlador.consultar(Campos.data(entrada, "Data de entrada"),
                    Campos.data(saida, "Data de saída"), codigo), resultado -> {
                modelo.setRowCount(0);
                for (Disponibilidade d : resultado)
                    modelo.addRow(new Object[]{d.tipo(), d.tipo().capacidadeMaxima(), Campos.moeda(d.tipo().valorDiaria()),
                            d.quartosOperacionais(), d.picoOcupacao(), d.livres()});
            });
        });

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(Campos.formulario("Entrada (dd/mm/aaaa)", entrada, "Saída (dd/mm/aaaa)", saida, "Tipo", tipo),
                BorderLayout.CENTER);
        norte.add(consultar, BorderLayout.SOUTH);
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        painel.add(norte, BorderLayout.NORTH);
        JScrollPane rolagem = new JScrollPane(Campos.tabela(modelo));
        rolagem.setPreferredSize(new Dimension(720, 160));
        painel.add(rolagem, BorderLayout.CENTER);
        painel.add(new JLabel("<html><small>Ocupação máxima: maior número simultâneo de reservas confirmadas e "
                + "hospedagens ativas em algum dia do período. Quartos em manutenção não contam.</small></html>"),
                BorderLayout.SOUTH);
        setContentPane(painel);
        pack();
        setLocationRelativeTo(dono);
        Tarefa.executar(consultar, controlador::listarTipos, lista -> lista.forEach(tipo::addItem));
    }
}
