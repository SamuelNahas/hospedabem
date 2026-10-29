package hospedabem.model;

import java.time.LocalDate;

public record Hospede(int id, String cpfOuPassaporte, String nome, LocalDate dataNascimento, String endereco,
                      String cidade, String estado, String telefone, String email) {
}
