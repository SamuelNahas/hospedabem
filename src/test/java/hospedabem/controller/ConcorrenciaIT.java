package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Duas conexões simultâneas disputando a última vaga do tipo, o mesmo quarto ou o último perfil de administrador. */
class ConcorrenciaIT extends IntegracaoBase {

    /** Executa as tarefas ao mesmo tempo, cada uma em sua thread e conexão, e conta os sucessos. */
    int sucessosSimultaneos(List<Callable<?>> tarefas) throws Exception {
        CyclicBarrier largada = new CyclicBarrier(tarefas.size());
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (Callable<?> t : tarefas) {
                resultados.add(pool.submit(() -> {
                    largada.await(5, TimeUnit.SECONDS);
                    try {
                        t.call();
                        return true;
                    } catch (RuntimeException e) {
                        return false;
                    }
                }));
            }
            int sucessos = 0;
            for (Future<Boolean> f : resultados) if (f.get(30, TimeUnit.SECONDS)) sucessos++;
            return sucessos;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void duasReservasNaoOcupamAUltimaVagaDoTipo() throws Exception {
        TipoQuarto t = criarTipo("VAGA", 2);
        criarQuarto(501, t);
        for (int rodada = 0; rodada < 5; rodada++) {
            int dia = 10 + rodada * 3;
            int sucessos = sucessosSimultaneos(List.of(
                    () -> reservas.registrar(CARLOS, "VAGA", HOJE.plusDays(dia), HOJE.plusDays(dia + 2), 1),
                    () -> reservas.registrar(BEATRIZ, "VAGA", HOJE.plusDays(dia + 1), HOJE.plusDays(dia + 3), 1)));
            assertEquals(1, sucessos);
        }
        assertEquals(5, contar("SELECT COUNT(*) FROM reserva WHERE tipo_quarto_id = ?", t.id()));
    }

    /**
     * Dois administradores rebaixando um ao outro ao mesmo tempo. Sem a trava das linhas de
     * administrador, cada transação vê o outro ainda como administrador, as duas gravam linhas
     * diferentes e o hotel fica sem ninguém para gerenciar funcionários. As duas sessões são
     * atendidas pelo service direto porque a {@link Sessao} é única por aplicação.
     */
    @Test
    void doisAdministradoresNaoSeRebaixamAoMesmoTempoDeixandoOHotelSemNenhum() throws Exception {
        Funcionario admin01 = funcionario("admin01");
        Funcionario marta = funcionarios.incluir(
                novoFuncionario("marta01", "000.000.003-53", Perfil.ADMINISTRADOR), "Marta@2026".toCharArray());

        // A janela da corrida é estreita: várias rodadas para que as duas transações cheguem a ler
        // uma à outra antes de qualquer commit.
        for (int rodada = 0; rodada < 20; rodada++) {
            jdbc.update("UPDATE funcionario SET perfil = 'ADMINISTRADOR' WHERE id IN (?, ?)",
                    admin01.id(), marta.id());
            int sucessos = sucessosSimultaneos(List.of(
                    () -> {
                        servicoFuncionario.alterar(comPerfil(marta, Perfil.RECEPCIONISTA), admin01);
                        return null;
                    },
                    () -> {
                        servicoFuncionario.alterar(comPerfil(admin01, Perfil.RECEPCIONISTA), marta);
                        return null;
                    }));
            assertEquals(1, sucessos, "rodada " + rodada);
            assertEquals(1, contar("SELECT COUNT(*) FROM funcionario WHERE perfil = 'ADMINISTRADOR'"),
                    "rodada " + rodada);
        }
    }

    private static Funcionario comPerfil(Funcionario f, Perfil perfil) {
        return Funcionario.informado(f.id(), f.matricula(), f.cpf(), f.nome(), f.endereco(), f.cidade(), f.estado(),
                f.telefone(), f.email(), f.dataContratacao(), perfil);
    }

    @Test
    void doisCheckInsNaoOcupamOMesmoQuarto() throws Exception {
        TipoQuarto t = criarTipo("MESMO", 2);
        criarQuarto(502, t);
        criarQuarto(503, t);
        ControladorCheckIn a = checkIns.getObject();
        ControladorCheckIn b = checkIns.getObject();
        a.iniciarCheckInSemReserva(CARLOS, "MESMO", HOJE, HOJE.plusDays(2));
        a.adicionarHospede(CARLOS);
        a.selecionarQuarto(502);
        b.iniciarCheckInSemReserva(BEATRIZ, "MESMO", HOJE, HOJE.plusDays(2));
        b.adicionarHospede(BEATRIZ);
        b.selecionarQuarto(502);

        assertEquals(1, sucessosSimultaneos(List.of(a::confirmarCheckIn, b::confirmarCheckIn)));
        assertEquals(1, contar("SELECT COUNT(*) FROM hospedagem"));
        assertEquals(1, contar("SELECT COUNT(*) FROM conta_hospedagem"));
    }

    @Test
    void checkInSemReservaEReservaNaoDisputamAUltimaVaga() throws Exception {
        TipoQuarto t = criarTipo("MISTO", 2);
        criarQuarto(504, t);
        ControladorCheckIn c = checkIns.getObject();
        c.iniciarCheckInSemReserva(CARLOS, "MISTO", HOJE, HOJE.plusDays(2));
        c.adicionarHospede(CARLOS);
        c.selecionarQuarto(504);

        assertEquals(1, sucessosSimultaneos(List.of(c::confirmarCheckIn,
                () -> reservas.registrar(BEATRIZ, "MISTO", HOJE, HOJE.plusDays(1), 1))));
        assertEquals(1, contar("SELECT COUNT(*) FROM hospedagem") + contar(
                "SELECT COUNT(*) FROM reserva WHERE tipo_quarto_id = ?", t.id()));
    }
}
