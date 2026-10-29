package hospedabem.view;

import hospedabem.model.NegocioException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;

/** Converte exceções em mensagens para o funcionário, sem stack trace nem detalhes de conexão. */
public final class Mensagens {
    private Mensagens() {
    }

    public static String de(Throwable erro) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            if (t instanceof NegocioException) return t.getMessage();
            if (t instanceof DataAccessResourceFailureException || t instanceof CannotCreateTransactionException)
                return "Não foi possível estabelecer a conexão com o banco de dados (limite de 10 s). "
                        + "A operação não foi gravada.";
            if (t instanceof QueryTimeoutException || t instanceof PessimisticLockingFailureException)
                return "O banco de dados não respondeu no tempo limite ou houve conflito com outra operação "
                        + "simultânea. Nada foi gravado; tente novamente.";
            if (t instanceof DataAccessException)
                return "Erro ao acessar o banco de dados. A operação não foi gravada.";
        }
        return "Erro inesperado. A operação não foi concluída.";
    }
}
