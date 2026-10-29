package hospedabem.view;

import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.controller.ControladorReserva;
import hospedabem.controller.Disponibilidade;
import hospedabem.model.Reserva;
import hospedabem.model.TipoQuarto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.time.LocalDate;

/** UC08 Registrar Reserva. O código é gerado pelo banco e o funcionário é o da sessão. */
public class TelaReserva extends JDialog {

    public TelaReserva(JFrame dono, ControladorReserva controlador, ControladorDisponibilidade consultas) {
        super(dono, "Registrar reserva", true);
        JTextField cpf = new JTextField(14);
        JLabel nome = new JLabel(" ");
        JComboBox<TipoQuarto> tipo = new JComboBox<>();
        JTextField entrada = new JTextField(Campos.texto(LocalDate.now()), 10);
        JTextField saida = new JTextField(Campos.texto(LocalDate.now().plusDays(1)), 10);
        JTextField quantidade = new JTextField("1", 4);
        JLabel resultado = new JLabel(" ");
        JButton buscar = new JButton("Buscar hóspede");
        JButton verificar = new JButton("Verificar disponibilidade");
        JButton registrar = new JButton("Registrar reserva");

        buscar.addActionListener(e -> Tarefa.executar(buscar, () -> controlador.buscarResponsavel(cpf.getText()),
                h -> nome.setText(h.nome() + " - tel. " + h.telefone())));
        verificar.addActionListener(e -> {
            TipoQuarto t = (TipoQuarto) tipo.getSelectedItem();
            Tarefa.executar(verificar, () -> consultas.consultar(Campos.data(entrada, "Data de entrada"),
                    Campos.data(saida, "Data de saída"), t == null ? null : t.codigo()).get(0), d -> resultado.setText(
                    texto(d)));
        });
        registrar.addActionListener(e -> {
            TipoQuarto t = (TipoQuarto) tipo.getSelectedItem();
            Tarefa.executar(registrar, () -> controlador.registrar(cpf.getText(), t == null ? null : t.codigo(),
                    Campos.data(entrada, "Data de entrada"), Campos.data(saida, "Data de saída"),
                    Campos.inteiro(quantidade, "Quantidade de hóspedes")), r -> {
                resultado.setText("Reserva " + r.getCodigo() + " registrada.");
                Tarefa.info(registrar, resumo(r));
            });
        });

        JPanel botoes = new JPanel();
        botoes.add(verificar);
        botoes.add(registrar);
        JPanel sul = new JPanel(new BorderLayout());
        sul.add(resultado, BorderLayout.NORTH);
        sul.add(botoes, BorderLayout.SOUTH);
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        painel.add(Campos.formulario("CPF/passaporte do responsável", cpf, "", buscar, "Responsável", nome,
                "Tipo de quarto", tipo, "Entrada prevista (dd/mm/aaaa)", entrada, "Saída prevista (dd/mm/aaaa)", saida,
                "Quantidade de hóspedes", quantidade), BorderLayout.CENTER);
        painel.add(sul, BorderLayout.SOUTH);
        setContentPane(painel);
        pack();
        setLocationRelativeTo(dono);
        Tarefa.executar(registrar, consultas::listarTipos, lista -> lista.forEach(tipo::addItem));
    }

    private static String texto(Disponibilidade d) {
        return d.livres() > 0
                ? d.livres() + " quarto(s) " + d.tipo().codigo() + " disponível(is). Capacidade " + d.tipo().capacidadeMaxima()
                + ", diária " + Campos.moeda(d.tipo().valorDiaria()) + "."
                : "Sem disponibilidade do tipo " + d.tipo().codigo() + " no período.";
    }

    private static String resumo(Reserva r) {
        return "Reserva " + r.getCodigo() + " registrada.\nResponsável: " + r.getResponsavel().nome()
                + "\nTipo: " + r.getTipo() + "\nPeríodo: " + Campos.texto(r.getPeriodo().entrada()) + " a "
                + Campos.texto(r.getPeriodo().saida()) + "\nHóspedes: " + r.getQuantidadeHospedes();
    }
}
