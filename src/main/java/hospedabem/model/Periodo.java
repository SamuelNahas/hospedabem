package hospedabem.model;

import java.time.LocalDate;

public record Periodo(LocalDate entrada, LocalDate saida) {

    public Periodo {
        if (entrada == null || saida == null) throw new NegocioException("Informe as datas de entrada e de saída.");
        if (!saida.isAfter(entrada)) throw new NegocioException("Período inválido: a saída deve ser posterior à entrada.");
    }

    public boolean sobrepoe(Periodo outro) {
        return entrada.isBefore(outro.saida) && outro.entrada.isBefore(saida);
    }
}
