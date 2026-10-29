package hospedabem.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Executa acesso ao banco fora da thread de eventos do Swing e apresenta o resultado ou o erro nela. */
public final class Tarefa {
    private static final Logger LOG = LoggerFactory.getLogger(Tarefa.class);

    private Tarefa() {
    }

    public static <T> void executar(JComponent origem, Callable<T> trabalho, Consumer<T> aoConcluir) {
        origem.setEnabled(false);
        cursor(origem, Cursor.WAIT_CURSOR);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabalho.call();
            }

            @Override
            protected void done() {
                origem.setEnabled(true);
                cursor(origem, Cursor.DEFAULT_CURSOR);
                try {
                    aoConcluir.accept(get());
                } catch (ExecutionException e) {
                    LOG.warn("Operação recusada ou falhou: {}", e.getCause().toString());
                    LOG.debug("Detalhes", e.getCause());
                    erro(origem, Mensagens.de(e.getCause()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** A origem pode ainda não estar numa janela quando a tela dispara a carga inicial no construtor. */
    private static void cursor(JComponent origem, int tipo) {
        if (origem.getTopLevelAncestor() != null) origem.getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(tipo));
    }

    public static void erro(JComponent origem, String mensagem) {
        JOptionPane.showMessageDialog(origem, mensagem, "HospedaBem", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(JComponent origem, String mensagem) {
        JOptionPane.showMessageDialog(origem, mensagem, "HospedaBem", JOptionPane.INFORMATION_MESSAGE);
    }
}
