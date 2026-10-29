package hospedabem.model;

public record Disponibilidade(TipoQuarto tipo, int quartosOperacionais, int picoOcupacao) {

    public int livres() {
        return Math.max(0, quartosOperacionais - picoOcupacao);
    }

    public void exigirVaga() {
        if (livres() < 1)
            throw new NegocioException("Não há quarto do tipo " + tipo.codigo() + " disponível no período.");
    }
}
