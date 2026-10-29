package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessaoTest {
    static final Funcionario RECEPCIONISTA = new Funcionario(2, "recep01", "0", "Rui", LocalDate.now(), Perfil.RECEPCIONISTA);

    @Test
    void semSessaoTodaOperacaoEhNegada() {
        Sessao s = new Sessao();
        assertThrows(AcessoNegadoException.class, s::exigirQualquerPerfil);
        assertThrows(AcessoNegadoException.class, () -> s.exigir(Perfil.ADMINISTRADOR));
    }

    @Test
    void perfilForaDosPermitidosEhNegadoEPermitidoDevolveOFuncionario() {
        Sessao s = new Sessao();
        s.iniciar(RECEPCIONISTA);
        var negado = assertThrows(AcessoNegadoException.class, () -> s.exigir(Perfil.ADMINISTRADOR));
        assertEquals("Permissão insuficiente: operação restrita a [ADMINISTRADOR].", negado.getMessage());
        assertSame(RECEPCIONISTA, s.exigir(Perfil.ADMINISTRADOR, Perfil.RECEPCIONISTA));
        assertSame(RECEPCIONISTA, s.exigirQualquerPerfil());
    }

    @Test
    void encerrarRemoveOFuncionario() {
        Sessao s = new Sessao();
        s.iniciar(RECEPCIONISTA);
        s.encerrar();
        assertNull(s.getFuncionario());
        assertThrows(AcessoNegadoException.class, s::exigirQualquerPerfil);
    }
}
