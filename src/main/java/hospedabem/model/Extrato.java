package hospedabem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Composição do valor a pagar no check-out: diárias + consumos e serviços. */
public record Extrato(int quantidadeDiarias, BigDecimal valorDiaria, BigDecimal valorDiarias,
                      List<Consumo> consumos, BigDecimal valorConsumos, BigDecimal total) {
    /** Maior total que cabe em {@code pagamento.valor_total} (NUMERIC(15,2)). */
    public static final BigDecimal TOTAL_MAXIMO = new BigDecimal("9999999999999.99");

    public Extrato {
        consumos = List.copyOf(consumos);
        if (total.compareTo(TOTAL_MAXIMO) > 0)
            throw new NegocioException("O total da conta ultrapassa o maior valor que pode ser registrado. "
                    + "Procure o administrador.");
    }

    static Extrato calcular(BigDecimal valorDiaria, List<Consumo> consumos, LocalDateTime entrada,
                            LocalDateTime saida) {
        int diarias = RegraDiarias.contar(entrada, saida);
        BigDecimal valorDiarias = valorDiaria.multiply(BigDecimal.valueOf(diarias));
        BigDecimal valorConsumos = Consumo.somar(consumos);
        return new Extrato(diarias, valorDiaria, valorDiarias, consumos, valorConsumos, valorDiarias.add(valorConsumos));
    }
}
