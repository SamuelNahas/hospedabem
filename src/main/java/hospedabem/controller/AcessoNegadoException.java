package hospedabem.controller;

import hospedabem.model.NegocioException;

/** RF-7: acesso sem sessão ou sem o perfil exigido. */
public class AcessoNegadoException extends NegocioException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
