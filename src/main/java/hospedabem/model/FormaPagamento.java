package hospedabem.model;

/** Formas aceitas no check-out (RF-21). A confirmação é registrada no sistema, sem integração externa. */
public enum FormaPagamento {
    DINHEIRO("Dinheiro"), PIX("Pix"), CARTAO_CREDITO("Cartão de crédito"), CARTAO_DEBITO("Cartão de débito");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
