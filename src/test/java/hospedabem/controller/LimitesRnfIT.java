package hospedabem.controller;

import hospedabem.model.FormaPagamento;
import hospedabem.model.Hospedagem;
import hospedabem.model.ItemConsumo;
import hospedabem.model.Reserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RNF-1 (operação concluída ou recusada em até 10 s) e RNF-2 (disponibilidade em até 3 s) nas
 * operações da iteração 2, com um histórico de 20 000 reservas e 2 000 hospedagens encerradas
 * para que as consultas não rodem sobre tabelas quase vazias. Os tempos saem no log com o prefixo
 * {@code [RNF]}; é uma aferição local, não um teste de carga.
 */
class LimitesRnfIT extends IntegracaoBase {
    static final Duration RNF1 = Duration.ofSeconds(10);
    static final Duration RNF2 = Duration.ofSeconds(3);

    @Autowired ControladorItemConsumo itens;
    @Autowired ControladorConsumo consumos;
    @Autowired ControladorCheckOut checkOuts;

    @Test
    void operacoesDaIteracaoDoisFicamDentroDosLimites() {
        String configurado = jdbc.queryForObject("SELECT current_setting('statement_timeout') || ' / '"
                + " || current_setting('lock_timeout')", String.class);
        assertEquals("10s / 8s", configurado);
        criarHistorico();
        itens.incluir(new ItemConsumo(0, "AGUA", "Água mineral", "Frigobar", new BigDecimal("5.50")));
        Hospedagem h = checkInSemReserva(tipos.buscar("STD").orElseThrow(), 101, 2, CARLOS);
        Reserva r = reservas.registrar(BEATRIZ, "LUX", HOJE.plusDays(30), HOJE.plusDays(33), 2);

        Reserva alterada = medir("UC09 alterar reserva", RNF1, () -> reservas.alterar(r, BEATRIZ, "LUX",
                HOJE.plusDays(30), HOJE.plusDays(34), 2));
        medir("UC09 cancelar reserva", RNF1, () -> reservas.cancelar(alterada));
        medir("UC11 lançar consumo", RNF1, () -> consumos.lancar(h.getCodigo(), "AGUA", 1));
        BigDecimal total = medir("UC12 conferir conta", RNF1, () -> checkOuts.conferir(h.getCodigo())).total();
        medir("UC12 confirmar pagamento e check-out", RNF1,
                () -> checkOuts.confirmarPagamento(h.getCodigo(), FormaPagamento.CARTAO_DEBITO, total));
        medir("UC07 disponibilidade (todos os tipos, 90 dias)", RNF2,
                () -> disponibilidade.consultar(HOJE, HOJE.plusDays(90), null));
        sessao.encerrar();
        medir("UC01 login + RNF-8 troca de senha", RNF1, () -> {
            autenticacao.autenticar("recep01", "Recep@123".toCharArray());
            autenticacao.trocarSenha("Recep@123".toCharArray(), "Medida2026".toCharArray(), "Medida2026".toCharArray());
            return null;
        });
    }

    private void criarHistorico() {
        jdbc.update("""
                INSERT INTO reserva (hospede_responsavel_id, tipo_quarto_id, data_entrada_prevista, data_saida_prevista,
                                     quantidade_hospedes, situacao, funcionario_id)
                SELECT (SELECT id FROM hospede ORDER BY id LIMIT 1), (SELECT id FROM tipo_quarto WHERE codigo = 'SUI'),
                       CURRENT_DATE - 4000 + (i % 3900), CURRENT_DATE - 4000 + (i % 3900) + 2, 1,
                       (ARRAY['CONVERTIDA', 'CANCELADA'])[1 + i % 2], (SELECT id FROM funcionario ORDER BY id LIMIT 1)
                FROM generate_series(1, 20000) i""");
        jdbc.update("""
                INSERT INTO hospedagem (data_hora_entrada, data_saida_prevista, data_hora_saida, situacao, quarto_id,
                                        funcionario_id)
                SELECT now() - make_interval(days => 4000 - i), CURRENT_DATE - 3998 + i, now() - make_interval(days => 3998 - i),
                       'ENCERRADA', (SELECT id FROM quarto WHERE numero = 301), (SELECT id FROM funcionario ORDER BY id LIMIT 1)
                FROM generate_series(1, 2000) i""");
        jdbc.execute("ANALYZE");
    }

    private <T> T medir(String operacao, Duration limite, Supplier<T> acao) {
        long inicio = System.nanoTime();
        T resultado = acao.get();
        Duration gasto = Duration.ofNanos(System.nanoTime() - inicio);
        System.out.printf("[RNF] %-48s %6d ms (limite %d s)%n", operacao, gasto.toMillis(), limite.toSeconds());
        assertTrue(gasto.compareTo(limite) < 0, operacao + " levou " + gasto.toMillis() + " ms");
        return resultado;
    }
}
