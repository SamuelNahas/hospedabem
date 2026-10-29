package hospedabem.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaTest {

    @Test
    void hashConfereSomenteComASenhaCorreta() {
        String hash = Senha.gerarHash("Segredo@1".toCharArray());
        assertTrue(Senha.confere("Segredo@1".toCharArray(), hash));
        assertFalse(Senha.confere("segredo@1".toCharArray(), hash));
        assertFalse(hash.contains("Segredo"));
    }

    @Test
    void mesmaSenhaGeraHashesDiferentesPorCausaDoSalt() {
        assertNotEquals(Senha.gerarHash("x".toCharArray()), Senha.gerarHash("x".toCharArray()));
    }

    @Test
    void hashMalFormadoNaoConfere() {
        assertFalse(Senha.confere("x".toCharArray(), "texto-puro"));
    }
}
