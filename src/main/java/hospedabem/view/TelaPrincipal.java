package hospedabem.view;

import hospedabem.controller.ControladorAutenticacao;
import hospedabem.controller.ControladorCheckIn;
import hospedabem.controller.ControladorDisponibilidade;
import hospedabem.controller.ControladorQuarto;
import hospedabem.controller.ControladorReserva;
import hospedabem.controller.ControladorTipoQuarto;
import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** Menu da iteração 1: somente funcionalidades implementadas aparecem. */
public class TelaPrincipal extends JFrame {

    public TelaPrincipal(ConfigurableApplicationContext ctx, Funcionario funcionario) {
        super("HospedaBem - " + funcionario.nome() + " (" + funcionario.perfil() + ")");
        boolean admin = funcionario.perfil() == Perfil.ADMINISTRADOR;

        JButton tipos = new JButton("Tipos de quarto");
        JButton quartos = new JButton("Quartos");
        JButton disponibilidade = new JButton("Consultar disponibilidade");
        JButton reserva = new JButton("Registrar reserva");
        JButton checkIn = new JButton("Realizar check-in");
        JButton sair = new JButton("Sair");
        tipos.setEnabled(admin);
        quartos.setEnabled(admin);
        if (!admin) {
            tipos.setToolTipText("Restrito ao administrador (RF-3)");
            quartos.setToolTipText("Restrito ao administrador (RF-3)");
        }

        tipos.addActionListener(e -> new TelaTipoQuarto(this, ctx.getBean(ControladorTipoQuarto.class)).setVisible(true));
        quartos.addActionListener(e -> new TelaQuarto(this, ctx.getBean(ControladorQuarto.class),
                ctx.getBean(ControladorDisponibilidade.class)).setVisible(true));
        disponibilidade.addActionListener(e ->
                new TelaDisponibilidade(this, ctx.getBean(ControladorDisponibilidade.class)).setVisible(true));
        reserva.addActionListener(e -> new TelaReserva(this, ctx.getBean(ControladorReserva.class),
                ctx.getBean(ControladorDisponibilidade.class)).setVisible(true));
        checkIn.addActionListener(e -> new TelaCheckIn(this, ctx.getBean(ControladorCheckIn.class),
                ctx.getBean(ControladorDisponibilidade.class)).setVisible(true));
        sair.addActionListener(e -> {
            ctx.getBean(ControladorAutenticacao.class).sair();
            new TelaLogin(ctx).setVisible(true);
            dispose();
        });

        JPanel botoes = new JPanel(new GridLayout(0, 1, 6, 6));
        for (JButton b : new JButton[]{tipos, quartos, disponibilidade, reserva, checkIn, sair}) botoes.add(b);
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        painel.add(new JLabel("Matrícula " + funcionario.matricula() + " - perfil " + funcionario.perfil()),
                BorderLayout.NORTH);
        painel.add(botoes, BorderLayout.CENTER);
        setContentPane(painel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }
}
