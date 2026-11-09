package hospedabem.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

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

    private static final Set<String> UNIDADES_FEDERATIVAS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES",
            "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP",
            "SE", "TO");

    /** Idade máxima aceita; evita que um ano digitado errado passe como data de nascimento. */
    private static final int IDADE_MAXIMA = 130;

    public Hospede {
        if (cpfOuPassaporte == null || cpfOuPassaporte.isBlank())
            throw new NegocioException("CPF ou passaporte é obrigatório.");
        if (nome == null || nome.isBlank()) throw new NegocioException("Nome é obrigatório.");
        if (dataNascimento == null) throw new NegocioException("Data de nascimento é obrigatória.");
        if (telefone == null || telefone.isBlank()) throw new NegocioException("Telefone é obrigatório.");
    }

    /**
     * Hóspede a partir de dados informados pelo usuário: normaliza (documento sem pontuação e em
     * maiúsculas, UF em maiúsculas, opcionais em branco viram nulos) e recusa o que está fora de
     * formato ou maior que a coluna. Chamada pela tela, para o erro aparecer no campo, e de novo
     * pelo serviço, para nada chegar ao banco sem passar por aqui.
     */
    public static Hospede informado(int id, String cpfOuPassaporte, String nome, LocalDate dataNascimento,
                                    String endereco, String cidade, String estado, String telefone, String email) {
        return new Hospede(id, exigirDocumento(cpfOuPassaporte), exigirNome(nome), dataNascimento,
                limitar(opcional(endereco), 200, "Endereço"), limitar(opcional(cidade), 80, "Cidade"),
                exigirEstado(opcional(estado)), exigirTelefone(opcional(telefone)), exigirEmail(opcional(email)));
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

    /** Identificação nacional (CPF) quando só tem dígitos; caso contrário é passaporte. */
    public boolean ehCpf() {
        return ehCpf(cpfOuPassaporte);
    }

    /** Documento sem pontuação e em maiúsculas: a única grafia que vai para o banco e para a busca. */
    public static String normalizarDocumento(String documento) {
        return documento == null ? "" : documento.replaceAll("[\\s.\\-/]", "").toUpperCase(Locale.ROOT);
    }

    private static boolean ehCpf(String normalizado) {
        return !normalizado.isEmpty() && normalizado.chars().allMatch(Character::isDigit);
    }

    /** Aceita CPF do brasileiro ou passaporte do estrangeiro e devolve a grafia normalizada. */
    private static String exigirDocumento(String documento) {
        String d = normalizarDocumento(documento);
        if (d.isEmpty()) throw new NegocioException("CPF ou passaporte é obrigatório.");
        return ehCpf(d) ? exigirCpf(d) : exigirPassaporte(d);
    }

    /** Valida os dígitos verificadores; o CPF é guardado só com dígitos, como a coluna já tem. */
    private static String exigirCpf(String digitos) {
        if (digitos.length() != 11)
            throw new NegocioException("CPF deve ter 11 dígitos; para estrangeiro informe o passaporte.");
        if (digitos.chars().distinct().count() == 1) throw new NegocioException("CPF inválido.");
        for (int posicao = 9; posicao <= 10; posicao++) {
            int soma = 0;
            for (int i = 0; i < posicao; i++) soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
            int resto = soma % 11;
            int esperado = resto < 2 ? 0 : 11 - resto;
            if (digitos.charAt(posicao) - '0' != esperado) throw new NegocioException("CPF inválido.");
        }
        return digitos;
    }

    /** Passaporte: letras e números, com ao menos uma letra para não se confundir com CPF. */
    private static String exigirPassaporte(String passaporte) {
        if (!passaporte.chars().allMatch(Character::isLetterOrDigit))
            throw new NegocioException("Passaporte só aceita letras e números.");
        if (passaporte.length() < 5 || passaporte.length() > 20)
            throw new NegocioException("Passaporte deve ter de 5 a 20 letras e números, como XP1234567.");
        return passaporte;
    }

    private static String exigirNome(String nome) {
        String n = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (n.isEmpty()) throw new NegocioException("Nome é obrigatório.");
        if (n.length() < 3) throw new NegocioException("Nome deve ter ao menos 3 caracteres.");
        if (n.length() > 120) throw new NegocioException("Nome deve ter no máximo 120 caracteres.");
        return n;
    }

    private static String exigirEstado(String estado) {
        if (estado == null) return null;
        String uf = estado.toUpperCase(Locale.ROOT);
        if (!UNIDADES_FEDERATIVAS.contains(uf))
            throw new NegocioException("Estado inválido: use a sigla da unidade federativa, como MS.");
        return uf;
    }

    /** Telefone é obrigatório (a coluna é {@code NOT NULL}) e precisa ter DDD e número. */
    private static String exigirTelefone(String telefone) {
        if (telefone == null) throw new NegocioException("Telefone é obrigatório.");
        if (telefone.replaceAll("\\D", "").length() < 10)
            throw new NegocioException("Telefone inválido: informe DDD e número, como (67) 90000-0001.");
        return limitar(telefone, 20, "Telefone");
    }

    private static String exigirEmail(String email) {
        if (email == null) return null;
        if (!email.matches("[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+"))
            throw new NegocioException("E-mail inválido: use o formato nome@dominio.");
        return limitar(email, 120, "E-mail");
    }

    /** Recusa antes do banco, para a tela dizer qual campo passou do tamanho em vez de erro de SQL. */
    private static String limitar(String valor, int maximo, String nome) {
        if (valor != null && valor.length() > maximo)
            throw new NegocioException(nome + " deve ter no máximo " + maximo + " caracteres.");
        return valor;
    }

    private static String opcional(String valor) {
        if (valor == null) return null;
        String v = valor.trim();
        return v.isEmpty() ? null : v;
    }

    @Override
    public String toString() {
        return cpfOuPassaporte + " - " + nome;
    }
}
