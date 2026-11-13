package hospedabem.dao;

import hospedabem.model.Hospede;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HospedeDAOTest {

    @Test
    void documentoEhNormalizadoParaABusca() {
        assertEquals("11111111111", Hospede.normalizarDocumento("111.111.111-11"));
        assertEquals("XP1234567", Hospede.normalizarDocumento(" xp 123/4567 "));
        assertEquals("", Hospede.normalizarDocumento(null));
    }
}
