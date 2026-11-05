package hospedabem.service;

import hospedabem.dao.FuncionarioDAO;
import hospedabem.model.Funcionario;
import hospedabem.model.NegocioException;
import hospedabem.model.Perfil;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Duas estações gravando a mesma matrícula ou o mesmo CPF ao mesmo tempo passam as duas pela
 * pré-checagem e a segunda é barrada pelo {@code UNIQUE} do banco. A corrida não se monta num teste
 * de integração, então a tradução da violação em mensagem compreensível é exercitada aqui.
 */
class ServicoFuncionarioTest {
    private static final Funcionario MARTA = Funcionario.informado(0, "marta01", "000.000.003-53", "Marta Teste",
            null, null, null, null, null, LocalDate.of(2026, 11, 5), Perfil.RECEPCIONISTA);
    private static final Supplier<Void> CONFLITO = () -> {
        throw new DuplicateKeyException("duplicate key value violates unique constraint");
    };

    @Test
    void aViolacaoDoUniqueApontaOCampoRepetido() {
        assertEquals("Já existe funcionário com a matrícula marta01.",
                assertThrows(NegocioException.class,
                        () -> servico(true, false).traduzindoDuplicidade(MARTA, CONFLITO)).getMessage());
        assertEquals("Já existe funcionário com o CPF 000.000.003-53.",
                assertThrows(NegocioException.class,
                        () -> servico(false, true).traduzindoDuplicidade(MARTA, CONFLITO)).getMessage());
    }

    /** A outra estação pode ter desfeito a gravação antes da releitura; a mensagem ainda serve. */
    @Test
    void semAcharORepetidoNaReleituraAMensagemCitaOsDoisCampos() {
        assertEquals("Já existe funcionário com a matrícula marta01 ou com o CPF 000.000.003-53.",
                assertThrows(NegocioException.class,
                        () -> servico(false, false).traduzindoDuplicidade(MARTA, CONFLITO)).getMessage());
    }

    @Test
    void semViolacaoOResultadoDaGravacaoPassaDireto() {
        assertEquals("gravado", servico(false, false).traduzindoDuplicidade(MARTA, () -> "gravado"));
    }

    private static ServicoFuncionario servico(boolean matriculaRepetida, boolean cpfRepetido) {
        FuncionarioDAO dao = new FuncionarioDAO(null) {
            @Override
            public boolean existeMatricula(String matricula, int exceto) {
                return matriculaRepetida;
            }

            @Override
            public boolean existeCpf(String cpf, int exceto) {
                return cpfRepetido;
            }
        };
        return new ServicoFuncionario(dao, Clock.systemUTC(), new GerenciadorInerte());
    }

    /** Não há transação neste teste: só a tradução do erro é exercitada. */
    private static final class GerenciadorInerte implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definicao) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void commit(TransactionStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void rollback(TransactionStatus status) {
            throw new UnsupportedOperationException();
        }
    }
}
