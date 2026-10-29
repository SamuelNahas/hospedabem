package hospedabem.model;

/** Violação de regra de negócio; a mensagem é apresentada ao funcionário. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
