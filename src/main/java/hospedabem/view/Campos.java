package hospedabem.view;

import hospedabem.model.NegocioException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Leitura e formatação dos campos das telas. */
public final class Campos {
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private Campos() {
    }

    public static LocalDate data(JTextField campo, String nome) {
        try {
            return LocalDate.parse(campo.getText().trim(), DATA);
        } catch (DateTimeParseException e) {
            throw new NegocioException(nome + " inválida. Use o formato dd/mm/aaaa.");
        }
    }

    public static int inteiro(JTextField campo, String nome) {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new NegocioException(nome + " deve ser um número inteiro.");
        }
    }

    public static BigDecimal decimal(JTextField campo, String nome) {
        try {
            String t = campo.getText().trim();
            return new BigDecimal(t.contains(",") ? t.replace(".", "").replace(',', '.') : t);
        } catch (NumberFormatException e) {
            throw new NegocioException(nome + " inválido. Exemplo: 180,00");
        }
    }

    public static String texto(LocalDate d) {
        return d == null ? "" : DATA.format(d);
    }

    public static String moeda(BigDecimal v) {
        return MOEDA.format(v);
    }

    public static String decimalTexto(BigDecimal v) {
        return v.toPlainString().replace('.', ',');
    }

    /** Formulário de duas colunas: rótulo, componente, rótulo, componente... */
    public static JPanel formulario(Object... rotulosEComponentes) {
        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < rotulosEComponentes.length; i += 2) {
            c.gridy = i / 2;
            c.gridx = 0;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0;
            String texto = rotulosEComponentes[i].toString();
            JLabel rotulo = new JLabel(texto.isEmpty() ? "" : texto + ":");
            JComponent campo = (JComponent) rotulosEComponentes[i + 1];
            rotulo.setLabelFor(campo);
            painel.add(rotulo, c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            painel.add(campo, c);
        }
        return painel;
    }

    public static DefaultTableModel modeloSomenteLeitura(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    public static JTable tabela(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        return t;
    }
}
