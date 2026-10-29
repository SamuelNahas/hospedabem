package hospedabem.view;

import hospedabem.controller.ControladorTipoQuarto;
import hospedabem.model.NegocioException;
import hospedabem.model.TipoQuarto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
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

/** UC03 Gerenciar Tipo de Quarto: inclusão, listagem, busca, alteração e exclusão. */
public class TelaTipoQuarto extends JDialog {
    private final ControladorTipoQuarto controlador;
    private final DefaultTableModel modelo = Campos.modeloSomenteLeitura("Código", "Descrição", "Capacidade", "Diária");
    private final JTable tabela = Campos.tabela(modelo);
    private final JTextField codigo = new JTextField(10);
    private final JTextField descricao = new JTextField(25);
    private final JTextField capacidade = new JTextField(5);
    private final JTextField diaria = new JTextField(10);
    private List<TipoQuarto> tipos = new ArrayList<>();
    private TipoQuarto selecionado;

    public TelaTipoQuarto(JFrame dono, ControladorTipoQuarto controlador) {
        super(dono, "Tipos de quarto", true);
        this.controlador = controlador;

        JButton novo = new JButton("Novo");
        JButton salvar = new JButton("Salvar");
        JButton excluir = new JButton("Excluir");
        JButton buscar = new JButton("Buscar por código");
        novo.addActionListener(e -> selecionar(null));
        salvar.addActionListener(e -> salvar(salvar));
        excluir.addActionListener(e -> excluir(excluir));
        buscar.addActionListener(e -> Tarefa.executar(buscar, () -> controlador.buscar(codigo.getText()),
                encontrado -> encontrado.ifPresentOrElse(this::selecionar,
                        () -> Tarefa.info(buscar, "Nenhum tipo com o código " + codigo.getText() + "."))));
        tabela.getSelectionModel().addListSelectionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (!e.getValueIsAdjusting() && linha >= 0) selecionar(tipos.get(linha));
        });

        JPanel acoes = new JPanel();
        for (JButton b : new JButton[]{novo, salvar, excluir, buscar}) acoes.add(b);
        JPanel sul = new JPanel(new BorderLayout());
        sul.add(Campos.formulario("Código", codigo, "Descrição", descricao, "Capacidade máxima", capacidade,
                "Valor da diária (R$)", diaria), BorderLayout.CENTER);
        sul.add(acoes, BorderLayout.SOUTH);
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setPreferredSize(new Dimension(520, 200));
        painel.add(rolagem, BorderLayout.CENTER);
        painel.add(sul, BorderLayout.SOUTH);
        setContentPane(painel);
        pack();
        setLocationRelativeTo(dono);
        recarregar(novo);
    }

    private void recarregar(JButton origem) {
        Tarefa.executar(origem, controlador::listar, lista -> {
            tipos = lista;
            modelo.setRowCount(0);
            for (TipoQuarto t : lista)
                modelo.addRow(new Object[]{t.codigo(), t.descricao(), t.capacidadeMaxima(), Campos.moeda(t.valorDiaria())});
        });
    }

    private void selecionar(TipoQuarto t) {
        selecionado = t;
        codigo.setText(t == null ? "" : t.codigo());
        descricao.setText(t == null ? "" : t.descricao());
        capacidade.setText(t == null ? "" : String.valueOf(t.capacidadeMaxima()));
        diaria.setText(t == null ? "" : Campos.decimalTexto(t.valorDiaria()));
        if (t == null) tabela.clearSelection();
    }

    private void salvar(JButton origem) {
        TipoQuarto t;
        try {
            t = new TipoQuarto(selecionado == null ? 0 : selecionado.id(), codigo.getText(), descricao.getText(),
                    Campos.inteiro(capacidade, "Capacidade"), Campos.decimal(diaria, "Valor da diária"));
        } catch (NegocioException e) {
            Tarefa.erro(origem, e.getMessage());
            return;
        }
        Tarefa.executar(origem, () -> {
            if (selecionado == null) controlador.incluir(t);
            else controlador.alterar(t);
            return t;
        }, ok -> {
            Tarefa.info(origem, "Tipo " + t.codigo() + " gravado.");
            selecionar(null);
            recarregar(origem);
        });
    }

    private void excluir(JButton origem) {
        if (selecionado == null) {
            Tarefa.erro(origem, "Selecione um tipo na lista.");
            return;
        }
        TipoQuarto alvo = selecionado;
        if (JOptionPane.showConfirmDialog(this, "Excluir o tipo " + alvo.codigo() + "?", "Confirmar",
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
