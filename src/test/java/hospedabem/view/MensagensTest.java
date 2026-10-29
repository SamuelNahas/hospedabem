package hospedabem.view;

import hospedabem.model.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensagensTest {

    @Test
    void regraDeNegocioMostraAPropriaMensagemMesmoEncapsulada() {
        assertEquals("Período inválido.", Mensagens.de(new ExecutionException(new NegocioException("Período inválido."))));
    }

    @Test
    void falhaDeConexaoInformaQueNadaFoiGravado() {
        String m = Mensagens.de(new CannotGetJdbcConnectionException("x", new SQLException("senha=segredo")));
        assertTrue(m.startsWith("Não foi possível estabelecer a conexão com o banco de dados"));
        assertFalse(m.contains("segredo"));
    }

    @Test
    void tempoEsgotadoEBloqueioTemMensagemPropria() {
        assertTrue(Mensagens.de(new QueryTimeoutException("x")).startsWith("O banco de dados não respondeu"));
        assertTrue(Mensagens.de(new CannotAcquireLockException("x")).startsWith("O banco de dados não respondeu"));
    }

    @Test
    void outrasFalhasNaoExpoemDetalhes() {
        assertEquals("Erro ao acessar o banco de dados. A operação não foi gravada.",
                Mensagens.de(new DataIntegrityViolationException("constraint xyz")));
        assertEquals("Erro inesperado. A operação não foi concluída.", Mensagens.de(new IllegalStateException("bug")));
    }
}
