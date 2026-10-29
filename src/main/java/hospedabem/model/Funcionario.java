package hospedabem.model;

import java.time.LocalDate;

/** Dados cadastrais completos pertencem ao UC (gerenciar funcionário) da iteração 2. */
public record Funcionario(int id, String matricula, String cpf, String nome, LocalDate dataContratacao,
                          Perfil perfil) {
}
