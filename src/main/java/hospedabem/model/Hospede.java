package hospedabem.model;

import java.time.LocalDate;

/** RF-8. Endereço, cidade, estado e e-mail são opcionais (podem ser null). */
public record Hospede(int id, String cpfOuPassaporte, String nome, LocalDate dataNascimento, String endereco,
                      String cidade, String estado, String telefone, String email) {
}
