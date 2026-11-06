package hospedabem.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Regra de contagem de diárias do check-out (UC12), decidida pela equipe antes da implementação
 * porque o documento de requisitos não fixa horários-limite nem arredondamento:
 *
 * <ul>
 *   <li>Conta-se o número de noites: dias de calendário entre a data de entrada e a data de saída
 *       efetiva, sem considerar o horário.</li>
 *   <li>Saída no mesmo dia da entrada cobra uma diária (mínimo de 1).</li>
 *   <li>Saída antecipada cobra só as noites usadas, não o período previsto.</li>
 *   <li>Saída após a data prevista (atraso) cobra as noites efetivas, sem multa ou acréscimo por horário.</li>
 * </ul>
 */
public final class RegraDiarias {
    private RegraDiarias() {
    }

    public static int contar(LocalDateTime entrada, LocalDateTime saida) {
        if (saida.isBefore(entrada)) throw new NegocioException("A saída não pode ser anterior à entrada.");
        long noites = ChronoUnit.DAYS.between(entrada.toLocalDate(), saida.toLocalDate());
        return (int) Math.max(1, noites);
    }
}
