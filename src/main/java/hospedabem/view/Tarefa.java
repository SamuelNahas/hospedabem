package hospedabem.view;

import javafx.concurrent.Task;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Executa o acesso ao banco fora da thread do JavaFX e devolve o resultado ou o erro a ela. */
public final class Tarefa {
    private static final Logger LOG = LoggerFactory.getLogger(Tarefa.class);
    private static final AtomicInteger EM_ANDAMENTO = new AtomicInteger();

    private Tarefa() {
    }

    /** Tarefas ainda não concluídas; os testes de ponta a ponta esperam este número zerar. */
    static int emAndamento() {
        return EM_ANDAMENTO.get();
    }

    public static <T> void executar(Node origem, Aviso aviso, Callable<T> trabalho, Consumer<T> aoConcluir) {
        origem.setDisable(true);
        aviso.limpar();
        Task<T> tarefa = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabalho.call();
            }
        };
        tarefa.setOnSucceeded(e -> {
            try {
                origem.setDisable(false);
                aoConcluir.accept(tarefa.getValue());
            } finally {
                EM_ANDAMENTO.decrementAndGet();
            }
        });
        tarefa.setOnFailed(e -> {
            try {
                origem.setDisable(false);
                LOG.warn("Operação recusada ou falhou: {}", tarefa.getException().toString());
                LOG.debug("Detalhes", tarefa.getException());
                aviso.erro(Mensagens.de(tarefa.getException()));
            } finally {
                EM_ANDAMENTO.decrementAndGet();
            }
        });
        EM_ANDAMENTO.incrementAndGet();
        Thread thread = new Thread(tarefa, "hospedabem-tarefa");
        thread.setDaemon(true);
        thread.start();
    }
}
