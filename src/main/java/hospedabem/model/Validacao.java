package hospedabem.model;

import java.util.Locale;
import java.util.Set;

/** Regras de formato comuns aos cadastros de pessoas ({@link Funcionario} e {@link Hospede}). */
final class Validacao {
    private static final Set<String> UNIDADES_FEDERATIVAS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES",
            "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP",
            "SE", "TO");

    private Validacao() {
    }

    static String nome(String nome) {
        String n = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (n.isEmpty()) throw new NegocioException("Nome é obrigatório.");
        if (n.length() < 3) throw new NegocioException("Nome deve ter ao menos 3 caracteres.");
        if (n.length() > 120) throw new NegocioException("Nome deve ter no máximo 120 caracteres.");
        return n;
    }

    /** Confere os dígitos verificadores de um CPF já reduzido aos seus 11 dígitos. */
    static void exigirDigitosDoCpf(String digitos) {
        if (digitos.chars().distinct().count() == 1) throw new NegocioException("CPF inválido.");
        for (int posicao = 9; posicao <= 10; posicao++) {
            int soma = 0;
            for (int i = 0; i < posicao; i++) soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
            int resto = soma % 11;
            int esperado = resto < 2 ? 0 : 11 - resto;
            if (digitos.charAt(posicao) - '0' != esperado) throw new NegocioException("CPF inválido.");
        }
    }

    static String estado(String estado) {
        if (estado == null) return null;
        String uf = estado.toUpperCase(Locale.ROOT);
        if (!UNIDADES_FEDERATIVAS.contains(uf))
            throw new NegocioException("Estado inválido: use a sigla da unidade federativa, como MS.");
        return uf;
    }

    /**
     * Número nacional com DDD (ao menos 10 dígitos) ou internacional iniciado por "+" (de 8 a 15
     * dígitos, o limite do E.164), para não barrar o telefone de quem mora fora do Brasil. Aceita
     * só algarismos, espaço, parênteses, ponto e hífen, e um ramal no fim ("r123" ou "ramal 123").
     */
    static String telefone(String telefone) {
        if (telefone == null) return null;
        String numero = telefone.replaceFirst("(?i)\\s*(r|ramal)\\s*\\d+$", "");
        if (!numero.matches("\\+?[0-9 ().-]+"))
            throw new NegocioException("Telefone inválido: use só números, espaço, parênteses e hífen, "
                    + "como (67) 90000-0001.");
        int digitos = numero.replaceAll("\\D", "").length();
        boolean valido = telefone.startsWith("+") ? digitos >= 8 && digitos <= 15 : digitos >= 10;
        if (!valido)
            throw new NegocioException("Telefone inválido: informe DDD e número, como (67) 90000-0001, "
                    + "ou o número internacional com +, como +1 555 0100.");
        return limitar(telefone, 20, "Telefone");
    }

    static String email(String email) {
        if (email == null) return null;
        if (!email.matches("[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+"))
            throw new NegocioException("E-mail inválido: use o formato nome@dominio.");
        return limitar(email, 120, "E-mail");
    }

    /** Recusa antes do banco, para a tela dizer qual campo passou do tamanho em vez de erro de SQL. */
    static String limitar(String valor, int maximo, String nome) {
        if (valor != null && valor.length() > maximo)
            throw new NegocioException(nome + " deve ter no máximo " + maximo + " caracteres.");
        return valor;
    }

    static String opcional(String valor) {
        if (valor == null) return null;
        String v = valor.trim();
        return v.isEmpty() ? null : v;
    }
}
