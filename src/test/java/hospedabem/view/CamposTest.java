package hospedabem.view;

import hospedabem.model.NegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CamposTest {

    @Test
    void dataNoFormatoBrasileiroEstrito() {
        assertEquals(LocalDate.of(2026, 2, 28), Campos.data(" 28/02/2026 ", "Entrada"));
        var erro = assertThrows(NegocioException.class, () -> Campos.data("31/02/2026", "Entrada"));
        assertEquals("Entrada inválida. Use o formato dd/mm/aaaa.", erro.getMessage());
        assertThrows(NegocioException.class, () -> Campos.data("2026-02-01", "Entrada"));
        assertEquals("01/03/2026", Campos.texto(LocalDate.of(2026, 3, 1)));
        assertEquals("", Campos.texto(null));
    }

    @Test
    void inteiroEDecimalAceitamVirgulaEPonto() {
        assertEquals(12, Campos.inteiro(" 12 ", "Andar"));
        assertThrows(NegocioException.class, () -> Campos.inteiro("1,5", "Andar"));
        assertEquals(new BigDecimal("1234.50"), Campos.decimal("1.234,50", "Diária"));
        assertEquals(new BigDecimal("180.5"), Campos.decimal("180.5", "Diária"));
        assertThrows(NegocioException.class, () -> Campos.decimal("abc", "Diária"));
        assertEquals("180,50", Campos.decimalTexto(new BigDecimal("180.50")));
    }
}
