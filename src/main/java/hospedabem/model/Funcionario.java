package hospedabem.model;

import java.time.LocalDate;

public record Funcionario(int id, String matricula, String cpf, String nome, LocalDate dataContratacao,
                          Perfil perfil) {
}
