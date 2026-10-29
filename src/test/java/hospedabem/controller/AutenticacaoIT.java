package hospedabem.controller;

import hospedabem.model.NegocioException;
import hospedabem.model.Perfil;
import hospedabem.model.Quarto;
import hospedabem.model.SituacaoQuarto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC01 Autenticar-se e controle de acesso nos controladores (RF-1 a RF-7). */
class AutenticacaoIT extends IntegracaoBase {

    @Test
    void loginCorretoIniciaSessaoComOPerfil() {
        sessao.encerrar();
        assertEquals(Perfil.ADMINISTRADOR, autenticacao.autenticar("admin01", "Admin@123".toCharArray()).perfil());
        assertEquals(Perfil.RECEPCIONISTA, autenticacao.autenticar("recep01", "Recep@123".toCharArray()).perfil());
        assertEquals(Perfil.RECEPCIONISTA, perfilDaSessao());
    }

    @Test
    void senhaIncorretaOuMatriculaInexistenteSaoRecusadas() {
        sessao.encerrar();
        assertThrows(NegocioException.class, () -> autenticacao.autenticar("admin01", "errada".toCharArray()));
        assertThrows(NegocioException.class, () -> autenticacao.autenticar("ninguem", "Admin@123".toCharArray()));
        assertThrows(AcessoNegadoException.class, () -> disponibilidade.listarTipos());
    }

    @Test
    void senhasSaoArmazenadasComoHashPbkdf2() {
        assertEquals(0, contar("SELECT COUNT(*) FROM funcionario WHERE senha_hash NOT LIKE 'pbkdf2$210000$%' "
                + "OR senha_hash LIKE '%Admin@123%'"));
    }

    @Test
    void chamadaDiretaSemSessaoEhRecusada() {
        autenticacao.sair();
        assertThrows(AcessoNegadoException.class, () -> tipos.listar());
        assertThrows(AcessoNegadoException.class, () -> reservas.buscarResponsavel(CARLOS));
        assertThrows(AcessoNegadoException.class, () -> checkIns.getObject().iniciarCheckIn(1001));
    }

    @Test
    void recepcionistaNaoGerenciaTiposNemQuartosMesmoChamandoOControlador() {
        var tipo = tipos.listar().get(0);
        logarComo("recep01");
        assertThrows(AcessoNegadoException.class, () -> tipos.listar());
        assertThrows(AcessoNegadoException.class, () -> tipos.excluir(tipo));
        assertThrows(AcessoNegadoException.class,
                () -> quartos.incluir(new Quarto(0, 999, 9, tipo, SituacaoQuarto.DISPONIVEL)));
        assertEquals(0, contar("SELECT COUNT(*) FROM quarto WHERE numero = 999"));
    }

    @Test
    void recepcionistaConsultaDisponibilidade() {
        logarComo("recep01");
        assertTrue(disponibilidade.consultar(HOJE, HOJE.plusDays(1), null).size() >= 3);
    }

    @Test
    void senhaEhApagadaDaMemoriaAposALogin() {
        char[] senha = "Admin@123".toCharArray();
        autenticacao.autenticar("admin01", senha);
        assertEquals("\0".repeat(9), new String(senha));
    }

    @Test
    void funcionarioAutenticadoListaOsTipos() {
        logarComo("recep01");
        assertEquals(3, disponibilidade.listarTipos().size());
    }
}
