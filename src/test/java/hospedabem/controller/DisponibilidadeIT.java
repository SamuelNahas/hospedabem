package hospedabem.controller;

import hospedabem.model.NegocioException;
import hospedabem.model.Quarto;
import hospedabem.model.SituacaoQuarto;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** UC07 Consultar Disponibilidade de Quartos. */
class DisponibilidadeIT extends IntegracaoBase {

    int livres(String tipo, int de, int ate) {
        return disponibilidade.consultar(HOJE.plusDays(de), HOJE.plusDays(ate), tipo).get(0).livres();
    }

    @Test
    void periodoInvalidoETipoInexistenteSaoRecusados() {
        assertThrows(NegocioException.class, () -> disponibilidade.consultar(HOJE, HOJE, "STD"));
        assertThrows(NegocioException.class, () -> disponibilidade.consultar(HOJE.plusDays(2), HOJE, "STD"));
        assertThrows(NegocioException.class, () -> disponibilidade.consultar(HOJE, HOJE.plusDays(1), "NADA"));
    }

    @Test
    void periodoSobrepostoOcupaEAdjacenteNao() {
        TipoQuarto t = criarTipo("UM", 2);
        criarQuarto(801, t);
        reservas.registrar(CARLOS, "UM", HOJE.plusDays(1), HOJE.plusDays(3), 1);
        assertEquals(0, livres("UM", 2, 4));
        assertEquals(0, livres("UM", 0, 2));
        assertEquals(1, livres("UM", 3, 5)); // começa no dia da saída
        assertEquals(1, livres("UM", 0, 1)); // termina no dia da entrada
    }

    @Test
    void reservasQueNaoSeSobrepoemEntreSiNaoSaoSomadas() {
        TipoQuarto t = criarTipo("DOIS", 2);
        criarQuarto(802, t);
        criarQuarto(803, t);
        reservas.registrar(CARLOS, "DOIS", HOJE.plusDays(1), HOJE.plusDays(2), 1);
        reservas.registrar(BEATRIZ, "DOIS", HOJE.plusDays(2), HOJE.plusDays(3), 1);
        reservas.registrar(DIEGO, "DOIS", HOJE.plusDays(3), HOJE.plusDays(4), 1);
        assertEquals(1, livres("DOIS", 1, 4));
        reservas.registrar(ELISA, "DOIS", HOJE.plusDays(1), HOJE.plusDays(4), 1); // ainda cabe
        assertEquals(0, livres("DOIS", 1, 4));
    }

    @Test
    void quartoEmManutencaoNaoConta() {
        TipoQuarto t = criarTipo("MAN", 2);
        criarQuarto(804, t);
        quartos.incluir(new Quarto(0, 805, 8, t, SituacaoQuarto.MANUTENCAO));
        var d = disponibilidade.consultar(HOJE, HOJE.plusDays(1), "MAN").get(0);
        assertEquals(1, d.quartosOperacionais());
        assertEquals(1, d.livres());
    }

    @Test
    void reservaConvertidaNaoEhDescontadaDuasVezesECanceladaNaoConsome() {
        TipoQuarto t = criarTipo("CONV", 2);
        criarQuarto(806, t);
        criarQuarto(807, t);
        int codigo = reservas.registrar(CARLOS, "CONV", HOJE, HOJE.plusDays(2), 1).getCodigo();
        assertEquals(1, livres("CONV", 0, 2));

        ControladorCheckIn c = checkIns.getObject();
        c.iniciarCheckIn(codigo);
        c.adicionarHospede(CARLOS);
        c.selecionarQuarto(806);
        c.confirmarCheckIn();
        assertEquals(1, livres("CONV", 0, 2)); // hospedagem conta; a reserva convertida não

        int outra = reservas.registrar(BEATRIZ, "CONV", HOJE, HOJE.plusDays(2), 1).getCodigo();
        assertEquals(0, livres("CONV", 0, 2));
        jdbc.update("UPDATE reserva SET situacao = 'CANCELADA' WHERE codigo = ?", outra);
        assertEquals(1, livres("CONV", 0, 2));
    }

    @Test
    void hospedagemAtrasadaContinuaOcupandoAteSerEncerrada() {
        TipoQuarto t = criarTipo("ATR", 2);
        criarQuarto(808, t);
        checkInSemReserva(t, 808, 2, CARLOS);
        jdbc.update("UPDATE hospedagem SET data_hora_entrada = data_hora_entrada - interval '5 days', "
                + "data_saida_prevista = ?", HOJE.minusDays(1)); // saída prevista vencida, sem check-out
        assertEquals(0, livres("ATR", 0, 1));
        assertEquals(1, livres("ATR", 1, 3)); // política: considera liberação a partir de amanhã
        assertEquals("OCUPADO", situacaoQuarto(808));
        assertThrows(NegocioException.class, () -> checkInSemReserva(t, 808, 1, BEATRIZ));
    }

    @Test
    void consultaSemTipoTrazTodos() {
        LocalDate amanha = HOJE.plusDays(1);
        assertEquals(3, disponibilidade.consultar(HOJE, amanha, null).size());
    }
}
