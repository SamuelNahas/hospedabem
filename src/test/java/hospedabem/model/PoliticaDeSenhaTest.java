package hospedabem.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Composição mínima da senha (UC02 e RNF-8). */
class PoliticaDeSenhaTest {

    @Test
    void senhaAusenteOuVaziaEhRecusada() {
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida(null));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida(new char[0]));
    }

    @Test
    void senhaCurtaEhRecusadaEOLimiteExatoEhAceito() {
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("Senha1".toCharArray()));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("Senha12".toCharArray()));
        PoliticaDeSenha.exigirValida("Senha123".toCharArray());
    }

    @Test
    void espacosNaoContamNoMinimoDeCaracteres() {
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("        1a".toCharArray()));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("Senha 12".toCharArray()));
        PoliticaDeSenha.exigirValida("Senha 123".toCharArray());
    }

    @Test
    void senhaSemLetraOuSemNumeroEhRecusada() {
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("12345678".toCharArray()));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("senhasenha".toCharArray()));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("@@@@@@@@".toCharArray()));
        PoliticaDeSenha.exigirValida("Marta@2026".toCharArray());
    }

    @Test
    void senhaVenceNoventaDiasAposATrocaOuJaNoPrimeiroAcessoSeForProvisoria() {
        LocalDateTime troca = LocalDateTime.of(2026, 1, 1, 10, 0);
        assertEquals(troca.plusDays(PoliticaDeSenha.VALIDADE_EM_DIAS), PoliticaDeSenha.venceEm(troca, false));
        assertFalse(troca.isBefore(PoliticaDeSenha.venceEm(troca, true)));
    }

    @Test
    void trocaExigeConfirmacaoIgualSenhaValidaEDiferenteDaAtual() {
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirTroca("".toCharArray(),
                "Nova12345".toCharArray(), "Nova12345".toCharArray()));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirTroca("Atual123".toCharArray(),
                "Nova12345".toCharArray(), "Nova12346".toCharArray()));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirTroca("Atual123".toCharArray(),
                "curta1".toCharArray(), "curta1".toCharArray()));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirTroca("Atual123".toCharArray(),
                "Atual123".toCharArray(), "Atual123".toCharArray()));
        PoliticaDeSenha.exigirTroca("Atual123".toCharArray(), "Nova12345".toCharArray(), "Nova12345".toCharArray());
    }
}
