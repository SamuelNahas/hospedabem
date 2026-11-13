package hospedabem.model;

import java.time.LocalDate;
import java.util.Locale;

import static hospedabem.model.Validacao.limitar;
import static hospedabem.model.Validacao.opcional;

/**
 * Hóspede do hotel (UC06). As regras de formato e de obrigatoriedade dos campos ficam aqui, pelo
 * padrão Especialista na Informação, e por isso são testadas sem banco.
 *
 * <p>Há dois caminhos de criação, pelo mesmo motivo do {@link Funcionario}. {@link #informado}
 * normaliza e valida o que o usuário digitou e é o único usado para gravar. O construtor apenas
 * reconstitui uma linha já gravada e confere só o que o próprio esquema garante (campos
 * {@code NOT NULL}), porque as linhas da carga inicial da iteração 1 têm documentos fictícios que
 * não passam pelos dígitos verificadores e não podem derrubar a listagem, a reserva nem o check-in
 * de quem está nelas.
 *
 * <p>A identificação aceita CPF (nacional) ou passaporte (estrangeiro) e é guardada sem pontuação,
 * em maiúsculas, para não haver duas grafias do mesmo documento — é essa única grafia que a
 * restrição {@code UNIQUE} da coluna usa para impedir cadastro duplicado.
 */
public record Hospede(int id, String cpfOuPassaporte, String nome, LocalDate dataNascimento, String endereco,
                      String cidade, String estado, String telefone, String email) {

    /** Idade máxima aceita; evita que um ano digitado errado passe como data de nascimento. */
    private static final int IDADE_MAXIMA = 130;

    public Hospede {
        if (cpfOuPassaporte == null) throw new NegocioException("CPF ou passaporte é obrigatório.");
        if (nome == null) throw new NegocioException("Nome é obrigatório.");
        if (dataNascimento == null) throw new NegocioException("Data de nascimento é obrigatória.");
        if (telefone == null) throw new NegocioException("Telefone é obrigatório.");
    }

    /**
     * Hóspede a partir de dados informados pelo usuário: normaliza (documento sem pontuação e em
     * maiúsculas, UF em maiúsculas, opcionais em branco viram nulos) e recusa o que está fora de
     * formato ou maior que a coluna. Chamada pela tela, para o erro aparecer no campo, e de novo
     * pelo serviço, para nada chegar ao banco sem passar por aqui.
     */
    public static Hospede informado(int id, String cpfOuPassaporte, String nome, LocalDate dataNascimento,
                                    String endereco, String cidade, String estado, String telefone, String email) {
        return new Hospede(id, exigirDocumento(cpfOuPassaporte), Validacao.nome(nome), dataNascimento,
                limitar(opcional(endereco), 200, "Endereço"), limitar(opcional(cidade), 80, "Cidade"),
                Validacao.estado(opcional(estado)), exigirTelefone(opcional(telefone)),
                Validacao.email(opcional(email)));
    }

    /** Os mesmos dados deste hóspede, normalizados e validados como se tivessem sido digitados. */
    public Hospede validado() {
        return informado(id, cpfOuPassaporte, nome, dataNascimento, endereco, cidade, estado, telefone, email);
    }

    /** Os mesmos dados com o id recebido; usado para devolver o hóspede recém-inserido. */
    public Hospede comId(int novoId) {
        return new Hospede(novoId, cpfOuPassaporte, nome, dataNascimento, endereco, cidade, estado, telefone, email);
    }

    /**
     * A data de nascimento não pode ser futura nem absurdamente antiga; recebe o dia corrente para
     * não consultar o relógio no modelo.
     */
    public void exigirNascimentoAte(LocalDate hoje) {
        if (dataNascimento.isAfter(hoje))
            throw new NegocioException("Data de nascimento não pode ser futura.");
        if (dataNascimento.isBefore(hoje.minusYears(IDADE_MAXIMA)))
            throw new NegocioException("Data de nascimento inválida: confira o ano.");
    }

    /** Identificação nacional (CPF) quando tem exatamente 11 dígitos; caso contrário é passaporte. */
    public boolean ehCpf() {
        return ehCpf(cpfOuPassaporte);
    }

    /** Documento sem pontuação e em maiúsculas: a única grafia que vai para o banco e para a busca. */
    public static String normalizarDocumento(String documento) {
        return documento == null ? "" : documento.replaceAll("[\\s.\\-/]", "").toUpperCase(Locale.ROOT);
    }

    private static boolean ehCpf(String normalizado) {
        return normalizado.matches("[0-9]{11}");
    }

    /** Aceita CPF do brasileiro ou passaporte do estrangeiro e devolve a grafia normalizada. */
    private static String exigirDocumento(String documento) {
        String d = normalizarDocumento(documento);
        if (d.isEmpty()) throw new NegocioException("CPF ou passaporte é obrigatório.");
        return ehCpf(d) ? exigirCpf(d) : exigirPassaporte(d);
    }

    /** Valida os dígitos verificadores; o CPF é guardado só com dígitos, como a coluna já tem. */
    private static String exigirCpf(String digitos) {
        Validacao.exigirDigitosDoCpf(digitos);
        return digitos;
    }

    /**
     * Passaporte: só letras latinas sem acento e algarismos (há passaportes só numéricos, como os
     * britânicos), para que letras parecidas de outros alfabetos não driblem a restrição UNIQUE.
     */
    private static String exigirPassaporte(String passaporte) {
        if (!passaporte.matches("[A-Z0-9]*"))
            throw new NegocioException("Passaporte só aceita letras de A a Z e números.");
        if (passaporte.length() < 5 || passaporte.length() > 20)
            throw new NegocioException("Passaporte deve ter de 5 a 20 letras e números, como XP1234567.");
        return passaporte;
    }

    /** Telefone é obrigatório (a coluna é {@code NOT NULL}). */
    private static String exigirTelefone(String telefone) {
        if (telefone == null) throw new NegocioException("Telefone é obrigatório.");
        return Validacao.telefone(telefone);
    }

    @Override
    public String toString() {
        return cpfOuPassaporte + " - " + nome;
    }
}
