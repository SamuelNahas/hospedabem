package hospedabem.view;

import hospedabem.controller.ControladorAutenticacao;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/** UC01 Autenticar-se. */
public class TelaLogin extends JFrame {

    public TelaLogin(ConfigurableApplicationContext contexto) {
        super("HospedaBem - Login");
        ControladorAutenticacao controlador = contexto.getBean(ControladorAutenticacao.class);
        JTextField matricula = new JTextField(15);
        JPasswordField senha = new JPasswordField(15);
        JButton entrar = new JButton("Entrar");

        entrar.addActionListener(e -> {
            String m = matricula.getText();
            char[] s = senha.getPassword();
            senha.setText("");
            Tarefa.executar(entrar, () -> controlador.autenticar(m, s), funcionario -> {
                new TelaPrincipal(contexto, funcionario).setVisible(true);
                dispose();
            });
        });
        getRootPane().setDefaultButton(entrar);

        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        painel.add(new JLabel("HospedaBem - Sistema de Hospedagem", SwingConstants.CENTER), BorderLayout.NORTH);
        painel.add(Campos.formulario("Matrícula", matricula, "Senha", senha), BorderLayout.CENTER);
        painel.add(entrar, BorderLayout.SOUTH);
        setContentPane(painel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }
}
