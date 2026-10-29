package hospedabem.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcupacaoTest {
    static final LocalDate D = LocalDate.of(2026, 11, 10);

    static Periodo p(int de, int ate) {
        return new Periodo(D.plusDays(de), D.plusDays(ate));
    }

    @Test
    void periodoComSaidaIgualOuAnteriorAEntradaEhInvalido() {
        assertThrows(NegocioException.class, () -> p(2, 2));
        assertThrows(NegocioException.class, () -> p(3, 1));
        assertThrows(NegocioException.class, () -> new Periodo(null, D));
    }

    @Test
    void periodosAdjacentesNaoSeSobrepoem() {
        assertFalse(p(0, 2).sobrepoe(p(2, 4)));
        assertFalse(p(2, 4).sobrepoe(p(0, 2)));
    }

    @Test
    void periodosSobrepostosSeSobrepoem() {
        assertTrue(p(0, 3).sobrepoe(p(2, 5)));
        assertTrue(p(0, 10).sobrepoe(p(4, 5)));
    }

    @Test
    void estadiasQueNaoSeSobrepoemEntreSiContamUmaVez() {
        // três reservas tocam a consulta, mas nunca coexistem: somá-las rejeitaria pedido válido
        assertEquals(1, Ocupacao.pico(List.of(p(0, 1), p(1, 2), p(2, 3)), p(0, 3)));
    }

    @Test
    void picoConsideraCadaMudancaDeOcupacao() {
        assertEquals(2, Ocupacao.pico(List.of(p(0, 4), p(1, 2), p(3, 5)), p(0, 5)));
        assertEquals(3, Ocupacao.pico(List.of(p(0, 4), p(1, 3), p(2, 5)), p(0, 5)));
    }

    @Test
    void estadiasForaDaConsultaSaoIgnoradas() {
        assertEquals(0, Ocupacao.pico(List.of(p(0, 2), p(5, 7)), p(2, 5)));
    }
}
