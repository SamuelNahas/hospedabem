package hospedabem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Conta aberta no check-in (UC10), que recebe os consumos (UC11). */
public class ContaDaHospedagem {
    private int id;
    private SituacaoConta situacao;
    private final BigDecimal valorDiaria;
    private final List<Consumo> consumos = new ArrayList<>();

    public ContaDaHospedagem(BigDecimal valorDiaria) {
        this(0, SituacaoConta.ABERTA, valorDiaria);
    }

    /** Reconstitui uma conta gravada. */
    public ContaDaHospedagem(int id, SituacaoConta situacao, BigDecimal valorDiaria) {
        this.id = id;
        this.situacao = situacao;
        this.valorDiaria = valorDiaria;
    }

    public void exigirAberta() {
        if (situacao != SituacaoConta.ABERTA)
            throw new NegocioException("A conta desta hospedagem está fechada: não aceita lançamentos nem nova cobrança.");
    }

    Consumo lancar(ItemConsumo item, int quantidade, LocalDateTime agora) {
        exigirAberta();
        Consumo c = Consumo.de(item, quantidade, agora);
        consumos.add(c);
        return c;
    }

    /** Acrescenta um lançamento já gravado, ao reconstituir a conta. */
    public void adicionarRegistrado(Consumo c) {
        consumos.add(c);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public SituacaoConta getSituacao() { return situacao; }
    public BigDecimal getValorDiaria() { return valorDiaria; }
    public List<Consumo> getConsumos() { return Collections.unmodifiableList(consumos); }
}
