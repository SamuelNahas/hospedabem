package hospedabem.model;

/**
 * Regras de composição da senha, comuns à definição inicial (UC02) e à troca pelo próprio
 * funcionário (RNF-8). Ficam no modelo porque não dependem de banco nem de tela.
 */
public final class PoliticaDeSenha {
    public static final int MINIMO_DE_CARACTERES = 8;

    private PoliticaDeSenha() {
    }

    public static void exigirValida(char[] senha) {
        if (senha == null || senha.length == 0) throw new NegocioException("Informe a senha do funcionário.");
        if (senha.length < MINIMO_DE_CARACTERES)
            throw new NegocioException("A senha deve ter ao menos " + MINIMO_DE_CARACTERES + " caracteres.");
        boolean letra = false;
        boolean digito = false;
        for (char c : senha) {
            if (Character.isLetter(c)) letra = true;
            if (Character.isDigit(c)) digito = true;
        }
        if (!letra || !digito) throw new NegocioException("A senha deve conter ao menos uma letra e um número.");
    }
}
