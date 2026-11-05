package hospedabem.model;

import java.time.LocalDate;
import java.util.Set;

/**
 * Funcionário do hotel (UC02). As regras de formato e de obrigatoriedade dos campos ficam aqui,
 * pelo padrão Especialista na Informação, e por isso são testadas sem banco.
 *
 * <p>Há dois caminhos de criação, de propósito. {@link #informado} normaliza e valida o que o
 * usuário digitou e é o único usado para gravar. O construtor apenas reconstitui uma linha já
 * gravada e confere só o que o próprio esquema garante (campos {@code NOT NULL}), porque uma linha
 * da iteração 1, inserida por SQL antes destas regras existirem, não pode derrubar a listagem nem
 * impedir o login de quem está nela.
 */
public record Funcionario(int id, String matricula, String cpf, String nome, String endereco, String cidade,
                          String estado, String telefone, String email, LocalDate dataContratacao, Perfil perfil) {

    private static final Set<String> UNIDADES_FEDERATIVAS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES",
            "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP",
            "SE", "TO");

    public Funcionario {
        if (matricula == null || matricula.isBlank()) throw new NegocioException("Matrícula é obrigatória.");
        if (cpf == null || cpf.isBlank()) throw new NegocioException("CPF é obrigatório.");
        if (nome == null || nome.isBlank()) throw new NegocioException("Nome é obrigatório.");
        if (dataContratacao == null) throw new NegocioException("Data de contratação é obrigatória.");
        if (perfil == null) throw new NegocioException("Perfil é obrigatório.");
    }

    /**
     * Funcionário a partir de dados informados pelo usuário: normaliza (matrícula em minúsculas,
     * CPF formatado, UF em maiúsculas, campos em branco viram nulos) e recusa o que está fora de
     * formato ou maior que a coluna. Chamada pela tela, para o erro aparecer no campo, e de novo
     * pelo serviço, para nada chegar ao banco sem passar por aqui.
     */
    public static Funcionario informado(int id, String matricula, String cpf, String nome, String endereco,
                                        String cidade, String estado, String telefone, String email,
                                        LocalDate dataContratacao, Perfil perfil) {
        return new Funcionario(id, exigirMatricula(matricula), exigirCpf(cpf), exigirNome(nome),
                limitar(opcional(endereco), 200, "Endereço"), limitar(opcional(cidade), 80, "Cidade"),
                exigirEstado(opcional(estado)), exigirTelefone(opcional(telefone)), exigirEmail(opcional(email)),
                dataContratacao, perfil);
    }

    /** Os mesmos dados deste funcionário, normalizados e validados como se tivessem sido digitados. */
    public Funcionario validado() {
        return informado(id, matricula, cpf, nome, endereco, cidade, estado, telefone, email, dataContratacao, perfil);
    }

    /** A data de contratação não pode ser futura; recebe o dia corrente para não consultar o relógio no modelo. */
    public void exigirContratacaoAte(LocalDate hoje) {
        if (dataContratacao.isAfter(hoje))
            throw new NegocioException("Data de contratação não pode ser futura.");
    }

    public boolean ehAdministrador() {
        return perfil == Perfil.ADMINISTRADOR;
    }

    public boolean ehOMesmoQue(Funcionario outro) {
        return outro != null && outro.id == id;
    }

    /**
     * Exclusão feita por {@code autor}: ninguém se exclui, para a sessão não ficar sem o próprio dono
     * e para o administrador não perder por acidente o acesso que está usando.
     */
    public void exigirExclusaoPor(Funcionario autor) {
        if (ehOMesmoQue(autor))
            throw new NegocioException("Você não pode excluir o seu próprio cadastro (" + matricula
                    + "). Peça a outro administrador.");
    }

    /**
     * Alteração feita por {@code autor}, em que {@code this} são os dados novos e {@code perfilGravado}
     * é o perfil atualmente no banco: o administrador não rebaixa o próprio perfil na mesma sessão.
     */
    public void exigirAlteracaoPor(Funcionario autor, Perfil perfilGravado) {
        if (ehOMesmoQue(autor) && perfilGravado == Perfil.ADMINISTRADOR && !ehAdministrador())
            throw new NegocioException("Você não pode rebaixar o seu próprio perfil (" + matricula
                    + "). Peça a outro administrador.");
    }

    private static String exigirMatricula(String matricula) {
        String m = matricula == null ? "" : matricula.trim().toLowerCase();
        if (m.isEmpty()) throw new NegocioException("Matrícula é obrigatória.");
        if (m.length() < 3 || m.length() > 20)
            throw new NegocioException("Matrícula deve ter de 3 a 20 caracteres.");
        if (!m.chars().allMatch(Character::isLetterOrDigit))
            throw new NegocioException("Matrícula só aceita letras e números.");
        return m;
    }

    private static String exigirNome(String nome) {
        String n = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (n.isEmpty()) throw new NegocioException("Nome é obrigatório.");
        if (n.length() < 3) throw new NegocioException("Nome deve ter ao menos 3 caracteres.");
        if (n.length() > 120) throw new NegocioException("Nome deve ter no máximo 120 caracteres.");
        return n;
    }

    /** Valida os dígitos verificadores e devolve o CPF formatado, para não haver duas grafias do mesmo número. */
    private static String exigirCpf(String cpf) {
        String digitos = (cpf == null ? "" : cpf).replaceAll("\\D", "");
        if (digitos.isEmpty()) throw new NegocioException("CPF é obrigatório.");
        if (digitos.length() != 11) throw new NegocioException("CPF deve ter 11 dígitos.");
        if (digitos.chars().distinct().count() == 1) throw new NegocioException("CPF inválido.");
        for (int posicao = 9; posicao <= 10; posicao++) {
            int soma = 0;
            for (int i = 0; i < posicao; i++) soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
            int resto = soma % 11;
            int esperado = resto < 2 ? 0 : 11 - resto;
            if (digitos.charAt(posicao) - '0' != esperado) throw new NegocioException("CPF inválido.");
        }
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "." + digitos.substring(6, 9)
                + "-" + digitos.substring(9);
    }

    private static String exigirEstado(String estado) {
        if (estado == null) return null;
        String uf = estado.toUpperCase();
        if (!UNIDADES_FEDERATIVAS.contains(uf))
            throw new NegocioException("Estado inválido: use a sigla da unidade federativa, como MS.");
        return uf;
    }

    private static String exigirTelefone(String telefone) {
        if (telefone == null) return null;
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

    /** Recusa antes do banco, para a tela dizer qual campo passou do tamanho em vez de mostrar erro de SQL. */
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
        return matricula + " - " + nome;
    }
}
