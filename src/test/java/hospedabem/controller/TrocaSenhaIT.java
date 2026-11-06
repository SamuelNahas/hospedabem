package hospedabem.controller;

import hospedabem.model.NegocioException;
import hospedabem.model.Perfil;
import hospedabem.model.PoliticaDeSenha;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** RNF-8: troca da própria senha e renovação periódica, integrada ao UC01 e ao UC02. */
class TrocaSenhaIT extends IntegracaoBase {

    void logarDeVerdade(String matricula, String senha) {
        autenticacao.autenticar(matricula, senha.toCharArray());
    }

    @Test
    void trocaValidaGravaHashAtualizaDataELoginPassaAUsarANovaSenha() {
        jdbc.update("UPDATE funcionario SET senha_alterada_em = now() - interval '10 days' WHERE matricula = 'recep01'");
        logarDeVerdade("recep01", "Recep@123");
        assertFalse(autenticacao.senhaVencida());
        autenticacao.trocarSenha("Recep@123".toCharArray(), "NovaSenha1".toCharArray(), "NovaSenha1".toCharArray());
        assertEquals(1, contar("SELECT COUNT(*) FROM funcionario WHERE matricula = 'recep01' "
                + "AND senha_hash LIKE 'pbkdf2$210000$%' AND senha_hash NOT LIKE '%NovaSenha1%' "
                + "AND senha_alterada_em > now() - interval '1 minute' AND NOT senha_provisoria"));
        autenticacao.sair();
        assertThrows(NegocioException.class, () -> logarDeVerdade("recep01", "Recep@123"));
        logarDeVerdade("recep01", "NovaSenha1");
        assertEquals(Perfil.RECEPCIONISTA, perfilDaSessao());
    }

    @Test
    void entradasInvalidasNaoAlteramASenha() {
        logarDeVerdade("recep01", "Recep@123");
        String antes = jdbc.queryForObject("SELECT senha_hash FROM funcionario WHERE matricula = 'recep01'", String.class);
        assertEquals("Senha atual incorreta.", assertThrows(NegocioException.class, () -> autenticacao.trocarSenha(
                "Errada123".toCharArray(), "NovaSenha1".toCharArray(), "NovaSenha1".toCharArray())).getMessage());
        assertThrows(NegocioException.class, () -> autenticacao.trocarSenha(
                "Recep@123".toCharArray(), "curta1".toCharArray(), "curta1".toCharArray()));
        assertThrows(NegocioException.class, () -> autenticacao.trocarSenha(
                "Recep@123".toCharArray(), "NovaSenha1".toCharArray(), "NovaSenha2".toCharArray()));
        assertEquals(antes, jdbc.queryForObject("SELECT senha_hash FROM funcionario WHERE matricula = 'recep01'", String.class));
    }

    @Test
    void senhaVencidaBloqueiaOperacoesAteATroca() {
        jdbc.update("UPDATE funcionario SET senha_alterada_em = now() - interval '"
                + PoliticaDeSenha.VALIDADE_EM_DIAS + " days' WHERE matricula = 'recep01'");
        logarDeVerdade("recep01", "Recep@123");
        assertTrue(autenticacao.senhaVencida());
        assertThrows(AcessoNegadoException.class, () -> disponibilidade.listarTipos());
        assertThrows(AcessoNegadoException.class, () -> reservas.buscarResponsavel(CARLOS));
        autenticacao.trocarSenha("Recep@123".toCharArray(), "NovaSenha1".toCharArray(), "NovaSenha1".toCharArray());
        assertFalse(autenticacao.senhaVencida());
        assertEquals(3, disponibilidade.listarTipos().size());
    }

    @Test
    void senhaAindaValidaUmDiaAntesDoLimite() {
        jdbc.update("UPDATE funcionario SET senha_alterada_em = now() - interval '"
                + (PoliticaDeSenha.VALIDADE_EM_DIAS - 1) + " days' WHERE matricula = 'recep01'");
        logarDeVerdade("recep01", "Recep@123");
        assertFalse(autenticacao.senhaVencida());
        assertEquals(3, disponibilidade.listarTipos().size());
    }

    @Test
    void senhaDefinidaNoCadastroEhProvisoriaEExigeTrocaNoPrimeiroAcesso() {
        funcionarios.incluir(novoFuncionario("marta01", "529.982.247-25", Perfil.RECEPCIONISTA), "Inicial123".toCharArray());
        logarDeVerdade("marta01", "Inicial123");
        assertTrue(autenticacao.senhaVencida());
        assertThrows(AcessoNegadoException.class, () -> disponibilidade.listarTipos());
        autenticacao.trocarSenha("Inicial123".toCharArray(), "Propria123".toCharArray(), "Propria123".toCharArray());
        autenticacao.sair();
        logarDeVerdade("marta01", "Propria123");
        assertFalse(autenticacao.senhaVencida());

        logarComo("admin01");
        funcionarios.redefinirSenha(funcionario("marta01"), "Redefin123".toCharArray());
        logarDeVerdade("marta01", "Redefin123");
        assertTrue(autenticacao.senhaVencida());
    }

    @Test
    void trocaSemSessaoEhRecusada() {
        autenticacao.sair();
        assertThrows(AcessoNegadoException.class, () -> autenticacao.trocarSenha(
                "Recep@123".toCharArray(), "NovaSenha1".toCharArray(), "NovaSenha1".toCharArray()));
    }
}
