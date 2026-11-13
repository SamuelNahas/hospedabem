package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import hospedabem.model.Reserva;
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

    /**
     * UC09 × UC10: a alteração da reserva e o check-in da mesma reserva ao mesmo tempo. Os dois
     * bloqueiam o tipo de quarto antes da reserva, na mesma ordem, então nunca se esperam em
     * sentidos opostos; e os dois conferem a reserva depois de travá-la, então exatamente um passa.
     * Quem perde não grava nada pela metade: ou a reserva segue como estava, ou não há hospedagem.
     */
    @Test
    void alteracaoDeReservaECheckInDaMesmaReservaNaoAcontecemAoMesmoTempo() throws Exception {
        TipoQuarto t = criarTipo("DISP", 2);
        for (int i = 0; i < 8; i++) criarQuarto(510 + i, t);
        for (int rodada = 0; rodada < 5; rodada++) {
            Reserva reserva = reservas.registrar(CARLOS, "DISP", HOJE, HOJE.plusDays(2), 1);
            ControladorCheckIn c = checkIns.getObject();
            c.iniciarCheckIn(reserva.getCodigo());
            c.adicionarHospede(CARLOS);
            c.selecionarQuarto(510 + rodada);
            Reserva resumo = reservas.consultar(reserva.getCodigo());

            int sucessos = sucessosSimultaneos(List.of(c::confirmarCheckIn,
                    () -> reservas.alterar(resumo, CARLOS, "DISP", HOJE, HOJE.plusDays(4), 1)));
            assertEquals(1, sucessos, "rodada " + rodada);
            // A reserva acabou convertida (venceu o check-in) ou alterada e ainda confirmada, nunca as duas.
            String situacao = situacaoReserva(reserva.getCodigo());
            assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE codigo = ? AND (situacao = 'CONVERTIDA'"
                    + " AND data_saida_prevista = ? OR situacao = 'CONFIRMADA' AND data_saida_prevista = ?)",
                    reserva.getCodigo(), HOJE.plusDays(2), HOJE.plusDays(4)), "rodada " + rodada + ": " + situacao);
        }
        assertEquals(5, contar("SELECT COUNT(*) FROM reserva WHERE tipo_quarto_id = ?", t.id()));
    }

    /**
     * Duas reservas remarcadas ao mesmo tempo para a mesma janela com um único quarto. Sem o
     * bloqueio do tipo de destino, cada transação recalcula a disponibilidade sem ver a outra, as
     * duas se dão por disponíveis e o tipo fica com duas reservas para um quarto.
     */
    @Test
    void duasAlteracoesNaoRemarcamParaAUltimaVagaDoTipo() throws Exception {
        TipoQuarto t = criarTipo("REMA", 2);
        criarQuarto(520, t);
        // A janela da corrida é estreita: cada rodada usa um bloco de dias próprio, longe dos outros,
        // para que as duas transações cheguem a recalcular a disponibilidade antes de qualquer commit.
        for (int rodada = 0; rodada < 10; rodada++) {
            int base = rodada * 100;
            Reserva a = reservas.registrar(CARLOS, "REMA", HOJE.plusDays(base + 10), HOJE.plusDays(base + 12), 1);
            Reserva b = reservas.registrar(BEATRIZ, "REMA", HOJE.plusDays(base + 20), HOJE.plusDays(base + 22), 1);

            int sucessos = sucessosSimultaneos(List.of(
                    () -> reservas.alterar(a, CARLOS, "REMA", HOJE.plusDays(base + 40), HOJE.plusDays(base + 43), 1),
                    () -> reservas.alterar(b, BEATRIZ, "REMA", HOJE.plusDays(base + 41), HOJE.plusDays(base + 44), 1)));
            assertEquals(1, sucessos, "rodada " + rodada);
            assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE tipo_quarto_id = ? AND situacao = 'CONFIRMADA'"
                            + " AND data_entrada_prevista >= ? AND data_entrada_prevista < ?",
                    t.id(), HOJE.plusDays(base + 40), HOJE.plusDays(base + 50)), "rodada " + rodada);
        }
    }

    /** Simula duas estações migrando o mesmo banco e verifica que a reserva existente é preservada. */
    @Test
    void duasEstacoesAtualizamBancoDaIteracaoUmSemConflito() throws Exception {
        for (int rodada = 0; rodada < 3; rodada++) {
            jdbc.execute("DROP TABLE consumo");
            jdbc.execute("DROP TABLE item_consumo");
            assertEquals(2, sucessosSimultaneos(List.of(
                    inicializador::inicializarSeVazio, inicializador::inicializarSeVazio)));
            assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE codigo = 1001"));
        }
    }
}
