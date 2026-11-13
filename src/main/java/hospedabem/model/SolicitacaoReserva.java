package hospedabem.model;

import java.time.LocalDate;

public record SolicitacaoReserva(Hospede responsavel, TipoQuarto tipo, Periodo periodo, int quantidadeHospedes) {

    public void validar(LocalDate hoje) {
        exigirEntradaNaoPassada(hoje);
        validarDados();
    }

    /**
     * Regras que valem igualmente no registro (UC08) e na alteração (UC09). A entrada fica fora
     * porque a alteração pode manter uma entrada que já passou sem remarcá-la para trás.
     */
    public void validarDados() {
        if (quantidadeHospedes <= 0) throw new NegocioException("Quantidade de hóspedes deve ser positiva.");
        tipo.exigirComporta(quantidadeHospedes);
    }

    public void exigirEntradaNaoPassada(LocalDate hoje) {
        if (periodo.entrada().isBefore(hoje)) throw new NegocioException("A data de entrada não pode estar no passado.");
    }
}
