package hospedabem.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lançamento de um item ou serviço na conta (UC11). Guarda código, descrição e preço unitário
 * aplicados, para a cobrança não mudar quando o catálogo for alterado (RF-7).
 */
public record Consumo(int id, String codigoItem, String descricaoItem, int quantidade, BigDecimal precoUnitario,
                      LocalDateTime dataHora) {
    public static final int QUANTIDADE_MAXIMA = 999;

    public Consumo {
        if (quantidade <= 0) throw new NegocioException("Quantidade deve ser maior que zero.");
        if (quantidade > QUANTIDADE_MAXIMA)
            throw new NegocioException("Quantidade não pode ultrapassar " + QUANTIDADE_MAXIMA + ".");
        if (precoUnitario == null || precoUnitario.signum() <= 0)
            throw new NegocioException("Preço unitário do lançamento deve ser positivo.");
        if (dataHora == null) throw new NegocioException("Data e hora do lançamento são obrigatórias.");
        precoUnitario = precoUnitario.setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Novo lançamento com o item e o preço atuais do catálogo, que passam a pertencer ao lançamento. */
    public static Consumo de(ItemConsumo item, int quantidade, LocalDateTime agora) {
        if (item == null) throw new NegocioException("Selecione um item do catálogo.");
        return new Consumo(0, item.codigo(), item.descricao(), quantidade, item.preco(), agora);
    }

    /** Preço unitário × quantidade; o banco grava o mesmo cálculo em {@code consumo.valor_total}. */
    public BigDecimal valorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public static BigDecimal somar(List<Consumo> consumos) {
        return consumos.stream().map(Consumo::valorTotal).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public Consumo comId(int novoId) {
        return new Consumo(novoId, codigoItem, descricaoItem, quantidade, precoUnitario, dataHora);
    }
}
