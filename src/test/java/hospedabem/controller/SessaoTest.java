package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessaoTest {
    static final Funcionario RECEPCIONISTA = new Funcionario(2, "recep01", "000.000.002-72", "Rui Recepcionista", null, null, null, null, null, LocalDate.now(), Perfil.RECEPCIONISTA);

    @Test
    void semSessaoTodaOperacaoEhNegada() {
        Sessao s = new Sessao(Clock.systemDefaultZone());
        assertThrows(AcessoNegadoException.class, s::exigirQualquerPerfil);
        assertThrows(AcessoNegadoException.class, () -> s.exigir(Perfil.ADMINISTRADOR));
    }

    @Test
    void perfilForaDosPermitidosEhNegadoEPermitidoDevolveOFuncionario() {
        Sessao s = new Sessao(Clock.systemDefaultZone());
        s.iniciar(RECEPCIONISTA);
        var negado = assertThrows(AcessoNegadoException.class, () -> s.exigir(Perfil.ADMINISTRADOR));
        assertEquals("Permissão insuficiente: operação restrita a [ADMINISTRADOR].", negado.getMessage());
        assertSame(RECEPCIONISTA, s.exigir(Perfil.ADMINISTRADOR, Perfil.RECEPCIONISTA));
        assertSame(RECEPCIONISTA, s.exigirQualquerPerfil());
    }

    @Test
    void encerrarRemoveOFuncionario() {
        Sessao s = new Sessao(Clock.systemDefaultZone());
        s.iniciar(RECEPCIONISTA);
        s.encerrar();
        assertNull(s.getFuncionario());
        assertThrows(AcessoNegadoException.class, s::exigirQualquerPerfil);
    }

    @Test
    void senhaQueVenceComASessaoAbertaPassaABloquearAsOperacoes() {
        Instant[] agora = {Instant.parse("2026-11-06T12:00:00Z")};
        Clock relogio = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zona) { return this; }
            public Instant instant() { return agora[0]; }
        };
        Sessao s = new Sessao(relogio);
        s.iniciar(RECEPCIONISTA, LocalDateTime.ofInstant(agora[0], ZoneOffset.UTC).plusHours(1));
        assertEquals(RECEPCIONISTA, s.exigir(Perfil.RECEPCIONISTA));
        agora[0] = agora[0].plusSeconds(3600);
        assertThrows(AcessoNegadoException.class, () -> s.exigir(Perfil.RECEPCIONISTA));
        assertEquals(RECEPCIONISTA, s.exigirAutenticado());
    }
}
