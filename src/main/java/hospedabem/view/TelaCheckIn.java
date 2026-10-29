package hospedabem.view;

import hospedabem.controller.ControladorCheckIn;
import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.model.Hospedagem;
import hospedabem.model.Hospede;
import hospedabem.model.Quarto;
import hospedabem.model.Reserva;
import hospedabem.model.TipoQuarto;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * UC10 Realizar Check-in numa única janela com quatro etapas e a confirmação final (RNF-12: até 5 etapas).
 * Cada botão corresponde a um evento do DSS.
 */
public class TelaCheckIn extends JDialog {
    private final ControladorCheckIn controlador;
    private final JLabel dadosAtendimento = new JLabel(" ");
    private final DefaultListModel<String> participantes = new DefaultListModel<>();
    private final JList<String> listaParticipantes = new JList<>(participantes);
    private final DefaultTableModel modeloQuartos = Campos.modeloSomenteLeitura("Número", "Andar", "Tipo", "Capacidade", "Diária");
    private final JTable tabelaQuartos = Campos.tabela(modeloQuartos);
    private final JTextArea resumo = new JTextArea(7, 50);
    private List<Quarto> quartos = new ArrayList<>();

    public TelaCheckIn(JFrame dono, ControladorCheckIn controlador, ControladorDisponibilidade consultas) {
        super(dono, "Realizar check-in", true);
        this.controlador = controlador;
        resumo.setEditable(false);

        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        painel.add(etapaInicio(consultas));
        painel.add(etapaParticipantes());
        painel.add(etapaQuarto());
        painel.add(etapaConfirmacao());
        setContentPane(new JScrollPane(painel));
        pack();
        setLocationRelativeTo(dono);
    }

    private JComponent etapaInicio(ControladorDisponibilidade consultas) {
        JRadioButton comReserva = new JRadioButton("Com reserva", true);
        JRadioButton semReserva = new JRadioButton("Sem reserva");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(comReserva);
        grupo.add(semReserva);
        JTextField codigo = new JTextField(8);
        JTextField cpf = new JTextField(14);
        JComboBox<TipoQuarto> tipo = new JComboBox<>();
        JTextField entrada = new JTextField(Campos.texto(LocalDate.now()), 10);
        JTextField saida = new JTextField(Campos.texto(LocalDate.now().plusDays(1)), 10);
        JButton iniciar = new JButton("Iniciar check-in");

        iniciar.addActionListener(e -> {
            limparEtapas();
            if (comReserva.isSelected()) {
                Tarefa.executar(iniciar, () -> controlador.iniciarCheckIn(Campos.inteiro(codigo, "Código da reserva")),
                        r -> dadosAtendimento.setText(descrever(r)));
            } else {
                TipoQuarto t = (TipoQuarto) tipo.getSelectedItem();
                Tarefa.executar(iniciar, () -> controlador.iniciarCheckInSemReserva(cpf.getText(),
                        t == null ? null : t.codigo(), Campos.data(entrada, "Data de entrada"),
                        Campos.data(saida, "Data de saída")), h -> dadosAtendimento.setText(descreverSemReserva(h)));
            }
        });

        JPanel modos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modos.add(comReserva);
        modos.add(semReserva);
        JPanel etapa = new JPanel(new BorderLayout());
        etapa.setBorder(BorderFactory.createTitledBorder("1. Reserva ou dados do atendimento sem reserva"));
        etapa.add(modos, BorderLayout.NORTH);
        etapa.add(Campos.formulario("Código da reserva", codigo, "Sem reserva - CPF/passaporte do responsável", cpf,
                "Sem reserva - tipo de quarto", tipo, "Sem reserva - entrada (hoje)", entrada,
                "Sem reserva - saída prevista", saida), BorderLayout.CENTER);
        JPanel sul = new JPanel(new BorderLayout());
        sul.add(iniciar, BorderLayout.NORTH);
        sul.add(dadosAtendimento, BorderLayout.CENTER);
        etapa.add(sul, BorderLayout.SOUTH);
        Tarefa.executar(iniciar, consultas::listarTipos, lista -> lista.forEach(tipo::addItem));
        return etapa;
    }

    private JComponent etapaParticipantes() {
        JTextField documento = new JTextField(14);
        JButton adicionar = new JButton("Adicionar hóspede");
        JButton remover = new JButton("Remover selecionado");
        adicionar.addActionListener(e -> Tarefa.executar(adicionar, () -> controlador.adicionarHospede(documento.getText()),
                h -> {
                    documento.setText("");
                    atualizarParticipantes();
                }));
        remover.addActionListener(e -> {
            int i = listaParticipantes.getSelectedIndex();
            if (i >= 0) controlador.removerHospede(controlador.getParticipantes().get(i));
            atualizarParticipantes();
        });

        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linha.add(new JLabel("CPF/passaporte:"));
        linha.add(documento);
        linha.add(adicionar);
        linha.add(remover);
        JScrollPane rolagem = new JScrollPane(listaParticipantes);
        rolagem.setPreferredSize(new Dimension(500, 80));
        JPanel etapa = new JPanel(new BorderLayout());
        etapa.setBorder(BorderFactory.createTitledBorder("2. Hóspedes da estadia (inclua o responsável)"));
        etapa.add(linha, BorderLayout.NORTH);
        etapa.add(rolagem, BorderLayout.CENTER);
        return etapa;
    }

    private JComponent etapaQuarto() {
        JButton consultar = new JButton("Consultar quartos disponíveis");
        JButton selecionar = new JButton("Selecionar quarto");
        consultar.addActionListener(e -> Tarefa.executar(consultar, controlador::consultarQuartosDisponiveis, lista -> {
            quartos = lista;
            modeloQuartos.setRowCount(0);
            for (Quarto q : lista)
                modeloQuartos.addRow(new Object[]{q.getNumero(), q.getAndar(), q.getTipo(),
                        q.getTipo().capacidadeMaxima(), Campos.moeda(q.getTipo().valorDiaria())});
        }));
        selecionar.addActionListener(e -> {
            int linha = tabelaQuartos.getSelectedRow();
            if (linha < 0) {
                Tarefa.erro(selecionar, "Selecione um quarto na lista.");
                return;
            }
            int numero = quartos.get(linha).getNumero();
            Tarefa.executar(selecionar, () -> controlador.selecionarQuarto(numero), r -> resumo.setText(descrever(r)));
        });

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.add(consultar);
        botoes.add(selecionar);
        JScrollPane rolagem = new JScrollPane(tabelaQuartos);
        rolagem.setPreferredSize(new Dimension(500, 100));
        JPanel etapa = new JPanel(new BorderLayout());
        etapa.setBorder(BorderFactory.createTitledBorder("3. Quarto"));
        etapa.add(botoes, BorderLayout.NORTH);
        etapa.add(rolagem, BorderLayout.CENTER);
        return etapa;
    }

    private JComponent etapaConfirmacao() {
        JButton confirmar = new JButton("Confirmar check-in");
        confirmar.addActionListener(e -> Tarefa.executar(confirmar, controlador::confirmarCheckIn, h -> {
            String texto = descrever(h);
            limparEtapas();
            dadosAtendimento.setText(" ");
            resumo.setText(texto);
            Tarefa.info(confirmar, texto);
        }));
        JPanel etapa = new JPanel(new BorderLayout());
        etapa.setBorder(BorderFactory.createTitledBorder("4. Resumo e confirmação"));
        etapa.add(new JScrollPane(resumo), BorderLayout.CENTER);
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sul.add(Box.createHorizontalGlue());
        sul.add(confirmar);
        etapa.add(sul, BorderLayout.SOUTH);
        return etapa;
    }

    private void atualizarParticipantes() {
        participantes.clear();
        for (Hospede h : controlador.getParticipantes()) participantes.addElement(h.cpfOuPassaporte() + " - " + h.nome());
        modeloQuartos.setRowCount(0);
        resumo.setText("");
    }

    private void limparEtapas() {
        participantes.clear();
        modeloQuartos.setRowCount(0);
        quartos = new ArrayList<>();
        resumo.setText("");
    }

    private static String descrever(Reserva r) {
        return "<html>Reserva " + r.getCodigo() + " (" + r.getSituacao() + ")<br>Responsável: " + r.getResponsavel().nome()
                + " - " + r.getResponsavel().cpfOuPassaporte() + "<br>Tipo: " + r.getTipo() + " - capacidade "
                + r.getTipo().capacidadeMaxima() + "<br>Período previsto: " + Campos.texto(r.getPeriodo().entrada())
                + " a " + Campos.texto(r.getPeriodo().saida()) + " - hóspedes: " + r.getQuantidadeHospedes() + "</html>";
    }

    private String descreverSemReserva(Hospede h) {
        TipoQuarto t = controlador.getTipo();
        return "<html>Sem reserva<br>Responsável: " + h.nome() + " - " + h.cpfOuPassaporte() + "<br>Tipo: " + t
                + " - capacidade " + t.capacidadeMaxima() + " - diária " + Campos.moeda(t.valorDiaria())
                + "<br>Período: " + Campos.texto(controlador.getPeriodo().entrada()) + " a "
                + Campos.texto(controlador.getPeriodo().saida()) + "</html>";
    }

    private static String descrever(ControladorCheckIn.Resumo r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.reserva() == null ? "Check-in sem reserva" : "Reserva " + r.reserva().getCodigo()).append('\n');
        sb.append("Responsável: ").append(r.responsavel().nome()).append('\n');
        sb.append("Hóspedes: ");
        r.hospedes().forEach(h -> sb.append(h.nome()).append("; "));
        sb.append("\nQuarto: ").append(r.quarto().getNumero()).append(" (andar ").append(r.quarto().getAndar())
                .append(", ").append(r.quarto().getTipo()).append(")\n");
        sb.append("Período previsto: ").append(Campos.texto(r.periodo().entrada())).append(" a ")
                .append(Campos.texto(r.periodo().saida())).append('\n');
        sb.append("Diária: ").append(Campos.moeda(r.quarto().getTipo().valorDiaria()));
        return sb.toString();
    }

    private static String descrever(Hospedagem h) {
        return "Check-in realizado com sucesso.\nHospedagem: " + h.getCodigo()
                + "\nEntrada: " + Campos.DATA_HORA.format(h.getDataHoraEntrada())
                + "\nSaída prevista: " + Campos.texto(h.getDataSaidaPrevista())
                + "\nQuarto ocupado: " + h.getQuarto().getNumero()
                + "\nConta aberta: nº " + h.getConta().getId() + " (" + h.getConta().getSituacao() + ")";
    }
}
