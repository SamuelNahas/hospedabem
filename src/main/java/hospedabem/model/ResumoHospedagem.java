package hospedabem.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Linha da lista de hospedagens ativas, para localizar a hospedagem no UC11 e no UC12. */
public record ResumoHospedagem(int codigo, int numeroQuarto, String hospedes, LocalDateTime entrada,
                               LocalDate saidaPrevista) {
    @Override
    public String toString() {
        return "Hospedagem " + codigo + " - Quarto " + numeroQuarto;
    }
}
