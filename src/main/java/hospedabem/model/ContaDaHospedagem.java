package hospedabem.model;

import java.math.BigDecimal;

/** Conta aberta no check-in. Diárias, consumos e totais derivados entram com o check-out (iteração 2). */
public class ContaDaHospedagem {
    private int id;
    private final SituacaoConta situacao = SituacaoConta.ABERTA;
    private final BigDecimal valorDiaria;

    public ContaDaHospedagem(BigDecimal valorDiaria) {
        this.valorDiaria = valorDiaria;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public SituacaoConta getSituacao() { return situacao; }
    public BigDecimal getValorDiaria() { return valorDiaria; }
}
