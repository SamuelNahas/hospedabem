package hospedabem.view;

import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.controller.ControladorQuarto;
import hospedabem.model.NegocioException;
import hospedabem.model.Quarto;
import hospedabem.model.SituacaoQuarto;
import hospedabem.model.TipoQuarto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** UC04 Gerenciar Quarto. */
public class TelaQuarto extends JDialog {
    private final ControladorQuarto controlador;
    private final DefaultTableModel modelo = Campos.modeloSomenteLeitura("Número", "Andar", "Tipo", "Situação");
    private final JTable tabela = Campos.tabela(modelo);
    private final JTextField numero = new JTextField(6);
    private final JTextField andar = new JTextField(4);
    private final JComboBox<TipoQuarto> tipo = new JComboBox<>();
    private final JComboBox<SituacaoQuarto> situacao = new JComboBox<>(SituacaoQuarto.values());
    private List<Quarto> quartos = new ArrayList<>();
    private Quarto selecionado;

    public TelaQuarto(JFrame dono, ControladorQuarto controlador, ControladorDisponibilidade consultas) {
        super(dono, "Quartos", true);
        this.controlador = controlador;

        JButton novo = new JButton("Novo");
        JButton salvar = new JButton("Salvar");
        JButton excluir = new JButton("Excluir");
        novo.addActionListener(e -> selecionar(null));
        salvar.addActionListener(e -> salvar(salvar));
        excluir.addActionListener(e -> excluir(excluir));
        tabela.getSelectionModel().addListSelectionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (!e.getValueIsAdjusting() && linha >= 0) selecionar(quartos.get(linha));
        });

        JPanel acoes = new JPanel();
        for (JButton b : new JButton[]{novo, salvar, excluir}) acoes.add(b);
        JPanel sul = new JPanel(new BorderLayout());
        sul.add(Campos.formulario("Número", numero, "Andar", andar, "Tipo", tipo, "Situação", situacao),
                BorderLayout.CENTER);
        sul.add(new JLabel("<html><small>Ocupado resulta do check-in. Reservas são por tipo: o quarto físico só é "
                + "definido no check-in, por isso \"reservado\" aparece na consulta de disponibilidade.</small></html>"),
                BorderLayout.NORTH);
        sul.add(acoes, BorderLayout.SOUTH);
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setPreferredSize(new Dimension(520, 220));
        painel.add(rolagem, BorderLayout.CENTER);
        painel.add(sul, BorderLayout.SOUTH);
        setContentPane(painel);
        pack();
        setLocationRelativeTo(dono);
        Tarefa.executar(novo, consultas::listarTipos, lista -> lista.forEach(tipo::addItem));
        recarregar(novo);
    }

    private void recarregar(JButton origem) {
        Tarefa.executar(origem, controlador::listar, lista -> {
            quartos = lista;
            modelo.setRowCount(0);
            for (Quarto q : lista) modelo.addRow(new Object[]{q.getNumero(), q.getAndar(), q.getTipo(), q.getSituacao()});
        });
    }

    private void selecionar(Quarto q) {
        selecionado = q;
        numero.setText(q == null ? "" : String.valueOf(q.getNumero()));
        andar.setText(q == null ? "" : String.valueOf(q.getAndar()));
        situacao.setSelectedItem(q == null ? SituacaoQuarto.DISPONIVEL : q.getSituacao());
        if (q != null) {
            for (int i = 0; i < tipo.getItemCount(); i++)
                if (tipo.getItemAt(i).id() == q.getTipo().id()) tipo.setSelectedIndex(i);
        } else {
            tabela.clearSelection();
        }
    }

    private void salvar(JButton origem) {
        Quarto q;
        try {
            q = new Quarto(selecionado == null ? 0 : selecionado.getId(), Campos.inteiro(numero, "Número"),
                    Campos.inteiro(andar, "Andar"), (TipoQuarto) tipo.getSelectedItem(),
                    (SituacaoQuarto) situacao.getSelectedItem());
        } catch (NegocioException e) {
            Tarefa.erro(origem, e.getMessage());
            return;
        }
        Tarefa.executar(origem, () -> {
            if (selecionado == null) controlador.incluir(q);
            else controlador.alterar(q);
            return q;
        }, ok -> {
            Tarefa.info(origem, "Quarto " + q.getNumero() + " gravado.");
            selecionar(null);
            recarregar(origem);
        });
    }

    private void excluir(JButton origem) {
        if (selecionado == null) {
            Tarefa.erro(origem, "Selecione um quarto na lista.");
            return;
        }
        Quarto alvo = selecionado;
        if (JOptionPane.showConfirmDialog(this, "Excluir o quarto " + alvo.getNumero() + "?", "Confirmar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Tarefa.executar(origem, () -> {
            controlador.excluir(alvo);
            return alvo;
        }, ok -> {
            selecionar(null);
            recarregar(origem);
        });
    }
}
