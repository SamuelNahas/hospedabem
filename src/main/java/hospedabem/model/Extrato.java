package hospedabem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Composição do valor a pagar no check-out: diárias + consumos e serviços. */
public record Extrato(int quantidadeDiarias, BigDecimal valorDiaria, BigDecimal valorDiarias,
                      List<Consumo> consumos, BigDecimal valorConsumos, BigDecimal total, LocalDateTime calculadoEm) {
    public Extrato {
        consumos = List.copyOf(consumos);
    }

    static Extrato calcular(BigDecimal valorDiaria, List<Consumo> consumos, LocalDateTime entrada,
                            LocalDateTime saida) {
        int diarias = RegraDiarias.contar(entrada, saida);
        BigDecimal valorDiarias = valorDiaria.multiply(BigDecimal.valueOf(diarias));
        BigDecimal valorConsumos = consumos.stream().map(Consumo::valorTotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new Extrato(diarias, valorDiaria, valorDiarias, consumos, valorConsumos,
                valorDiarias.add(valorConsumos), saida);
    }
}
