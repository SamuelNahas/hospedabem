package hospedabem.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HospedeDAOTest {

    @Test
    void documentoEhNormalizadoParaABusca() {
        assertEquals("11111111111", HospedeDAO.normalizar("111.111.111-11"));
        assertEquals("XP1234567", HospedeDAO.normalizar(" xp 123/4567 "));
        assertEquals("", HospedeDAO.normalizar(null));
    }
}
