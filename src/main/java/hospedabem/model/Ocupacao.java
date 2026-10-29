package hospedabem.model;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

public final class Ocupacao {
    private Ocupacao() {
    }

    // Varre cada mudança de ocupação: somar as estadias que tocam a consulta superestima a demanda.
    public static int pico(List<Periodo> estadias, Periodo consulta) {
        TreeMap<LocalDate, Integer> variacao = new TreeMap<>();
        for (Periodo p : estadias) {
            if (!p.sobrepoe(consulta)) continue;
            LocalDate inicio = p.entrada().isBefore(consulta.entrada()) ? consulta.entrada() : p.entrada();
            variacao.merge(inicio, 1, Integer::sum);
            variacao.merge(p.saida(), -1, Integer::sum);
        }
        int atual = 0;
        int maximo = 0;
        for (int delta : variacao.values()) {
            atual += delta;
            maximo = Math.max(maximo, atual);
        }
        return maximo;
    }
}
