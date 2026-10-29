package hospedabem.controller;

import hospedabem.model.TipoQuarto;

/** Resultado da consulta de um tipo: quartos operacionais, pico de ocupação no período e quartos livres. */
public record Disponibilidade(TipoQuarto tipo, int quartosOperacionais, int picoOcupacao) {
    public int livres() {
        return Math.max(0, quartosOperacionais - picoOcupacao);
    }
}
