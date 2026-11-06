package hospedabem.model;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Regras de senha, comuns à definição inicial (UC02) e à troca pelo próprio funcionário (RNF-8).
 *
 * <p>Política decidida pela equipe (o RNF-8 não fixa prazo nem composição):
 * <ul>
 *   <li>Composição: ao menos {@value #MINIMO_DE_CARACTERES} caracteres sem contar espaços, com pelo menos uma
 *       letra e um número.</li>
 *   <li>Validade: {@value #VALIDADE_EM_DIAS} dias contados de {@code senha_alterada_em}; a senha vence
 *       exatamente no instante em que esse prazo se completa.</li>
 *   <li>A senha definida pelo administrador (cadastro ou redefinição) é provisória e vence no primeiro acesso.</li>
 *   <li>Na troca, a nova senha deve ser confirmada e ser diferente da atual.</li>
 * </ul>
 */
public final class PoliticaDeSenha {
    public static final int MINIMO_DE_CARACTERES = 8;
    public static final int VALIDADE_EM_DIAS = 90;

    private PoliticaDeSenha() {
    }

    public static void exigirValida(char[] senha) {
        if (senha == null || senha.length == 0) throw new NegocioException("Informe a senha do funcionário.");
        int visiveis = 0;
        boolean letra = false;
        boolean digito = false;
        for (char c : senha) {
            if (!Character.isWhitespace(c)) visiveis++;
            if (Character.isLetter(c)) letra = true;
            if (Character.isDigit(c)) digito = true;
        }
        if (visiveis < MINIMO_DE_CARACTERES)
            throw new NegocioException("A senha deve ter ao menos " + MINIMO_DE_CARACTERES
                    + " caracteres, sem contar espaços.");
        if (!letra || !digito) throw new NegocioException("A senha deve conter ao menos uma letra e um número.");
    }

    /** Instante em que a senha vence: já vencida se provisória, senão {@value #VALIDADE_EM_DIAS} dias após a troca. */
    public static LocalDateTime venceEm(LocalDateTime alteradaEm, boolean provisoria) {
        return provisoria ? LocalDateTime.MIN : alteradaEm.plusDays(VALIDADE_EM_DIAS);
    }

    /** Confere os dados digitados na troca, antes de verificar a senha atual no banco. */
    public static void exigirTroca(char[] atual, char[] nova, char[] confirmacao) {
        if (atual == null || atual.length == 0) throw new NegocioException("Informe a senha atual.");
        if (nova == null || nova.length == 0) throw new NegocioException("Informe a nova senha.");
        if (confirmacao == null || !Arrays.equals(nova, confirmacao))
            throw new NegocioException("A confirmação não confere com a nova senha.");
        exigirValida(nova);
        if (Arrays.equals(atual, nova)) throw new NegocioException("A nova senha deve ser diferente da atual.");
    }
}
