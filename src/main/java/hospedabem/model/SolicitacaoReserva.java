package hospedabem.model;

import java.time.LocalDate;

public record SolicitacaoReserva(Hospede responsavel, TipoQuarto tipo, Periodo periodo, int quantidadeHospedes) {

    public void validar(LocalDate hoje) {
        if (periodo.entrada().isBefore(hoje)) throw new NegocioException("A data de entrada não pode estar no passado.");
        if (quantidadeHospedes <= 0) throw new NegocioException("Quantidade de hóspedes deve ser positiva.");
        tipo.exigirComporta(quantidadeHospedes);
    }
}
