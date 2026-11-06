package hospedabem.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Lançamento de um item ou serviço na conta (UC11). Guarda o preço unitário aplicado e o total
 * do lançamento, para a cobrança não mudar quando o catálogo for alterado (RF-7).
 */
public record Consumo(int id, String codigoItem, String descricaoItem, int quantidade, BigDecimal precoUnitario,
                      BigDecimal valorTotal, LocalDateTime dataHora) {
    public static final int QUANTIDADE_MAXIMA = 999;

    public Consumo {
        exigirQuantidade(quantidade);
        if (precoUnitario == null || precoUnitario.signum() <= 0)
            throw new NegocioException("Preço unitário do lançamento deve ser positivo.");
        if (dataHora == null) throw new NegocioException("Data e hora do lançamento são obrigatórias.");
        precoUnitario = precoUnitario.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal esperado = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        if (valorTotal == null || valorTotal.compareTo(esperado) != 0)
            throw new NegocioException("Total do lançamento não confere com quantidade × preço unitário.");
        valorTotal = esperado;
    }

    /** Novo lançamento com o preço atual do catálogo, que passa a pertencer ao lançamento. */
    public static Consumo de(ItemConsumo item, int quantidade, LocalDateTime agora) {
        if (item == null) throw new NegocioException("Selecione um item do catálogo.");
        exigirQuantidade(quantidade);
        return new Consumo(0, item.codigo(), item.descricao(), quantidade, item.preco(),
                item.preco().multiply(BigDecimal.valueOf(quantidade)), agora);
    }

    public static void exigirQuantidade(int quantidade) {
        if (quantidade <= 0) throw new NegocioException("Quantidade deve ser maior que zero.");
        if (quantidade > QUANTIDADE_MAXIMA)
            throw new NegocioException("Quantidade não pode ultrapassar " + QUANTIDADE_MAXIMA + ".");
    }

    public Consumo comId(int novoId) {
        return new Consumo(novoId, codigoItem, descricaoItem, quantidade, precoUnitario, valorTotal, dataHora);
    }
}
