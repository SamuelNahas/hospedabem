package hospedabem.controller;

import hospedabem.model.NegocioException;

public class AcessoNegadoException extends NegocioException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
