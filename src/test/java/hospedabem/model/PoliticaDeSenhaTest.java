package hospedabem.model;

import org.junit.jupiter.api.Test;

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
    void senhaSemLetraOuSemNumeroEhRecusada() {
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("12345678".toCharArray()));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("senhasenha".toCharArray()));
        assertThrows(NegocioException.class, () -> PoliticaDeSenha.exigirValida("@@@@@@@@".toCharArray()));
        PoliticaDeSenha.exigirValida("Marta@2026".toCharArray());
    }
}
