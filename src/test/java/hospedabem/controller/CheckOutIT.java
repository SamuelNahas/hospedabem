package hospedabem.controller;

import hospedabem.model.Extrato;
import hospedabem.model.FormaPagamento;
import hospedabem.model.Hospedagem;
import hospedabem.model.ItemConsumo;
import hospedabem.model.NegocioException;
import hospedabem.model.Pagamento;
import hospedabem.model.TipoQuarto;
import hospedabem.service.ServicoCheckOut;
import hospedabem.service.ServicoConsumo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** UC12 Realizar Check-out, contra o PostgreSQL de teste. */
class CheckOutIT extends IntegracaoBase {
    @Autowired ControladorConsumo consumos;
    @Autowired ControladorCheckOut checkOuts;
    @Autowired ControladorItemConsumo itens;
    @Autowired ServicoConsumo servicoConsumo;
    @Autowired ServicoCheckOut servicoCheckOut;
    @Autowired PlatformTransactionManager transacoes;

    TipoQuarto std;
    Hospedagem h;
    ItemConsumo agua;

    @BeforeEach
    void prepararHospedagem() {
        std = tipos.listar().stream().filter(t -> t.codigo().equals("STD")).findFirst().orElseThrow();
        agua = itens.incluir(new ItemConsumo(0, "AGUA", "Água mineral", "Frigobar", new BigDecimal("5.50")));
        h = checkInSemReserva(std, 101, 2, CARLOS);
        logarComo("recep01");
    }

    @Test
    void checkOutExigeSessaoDeRecepcionistaOuAdministrador() {
        sessao.encerrar();
        assertThrows(AcessoNegadoException.class, () -> checkOuts.listarHospedagensAtivas());
        assertThrows(AcessoNegadoException.class, () -> checkOuts.conferir(h.getCodigo()));
        assertThrows(AcessoNegadoException.class,
                () -> checkOuts.confirmarPagamento(h.getCodigo(), FormaPagamento.PIX, BigDecimal.TEN));
        assertEquals(0, contar("SELECT COUNT(*) FROM pagamento"));
    }


    @Test
    void totalComposicaoDiariasMaisConsumos() {
        consumos.lancar(h.getCodigo(), "AGUA", 3);
        jdbc.update("UPDATE hospedagem SET data_hora_entrada = data_hora_entrada - interval '2 days', "
                + "data_saida_prevista = data_saida_prevista WHERE codigo = ?", h.getCodigo());
        Extrato e = checkOuts.conferir(h.getCodigo());
        assertEquals(2, e.quantidadeDiarias());
        assertEquals(new BigDecimal("360.00"), e.valorDiarias());
        assertEquals(new BigDecimal("16.50"), e.valorConsumos());
        assertEquals(new BigDecimal("376.50"), e.total());
    }

    @ParameterizedTest
    @EnumSource(FormaPagamento.class)
    void confirmacaoGravaPagamentoEncerraFechaELiberaOQuarto(FormaPagamento forma) {
        consumos.lancar(h.getCodigo(), "AGUA", 1);
        Extrato e = checkOuts.conferir(h.getCodigo());
        assertEquals("OCUPADO", situacaoQuarto(101));
        Pagamento p = checkOuts.confirmarPagamento(h.getCodigo(), forma, e.total());
        assertEquals(e.total(), p.extrato().total());
        assertEquals(1, contar("SELECT COUNT(*) FROM pagamento WHERE forma = ? AND valor_total = ?", forma.name(), e.total()));
        assertEquals(1, contar("SELECT COUNT(*) FROM hospedagem WHERE codigo = ? AND situacao = 'ENCERRADA' "
                + "AND data_hora_saida IS NOT NULL", h.getCodigo()));
        assertEquals(1, contar("SELECT COUNT(*) FROM conta_hospedagem WHERE situacao = 'FECHADA' AND fechada_em IS NOT NULL"));
        assertEquals("DISPONIVEL", situacaoQuarto(101));
        assertTrue(consumos.listarHospedagensAtivas().isEmpty());
        // A liberação reflete na disponibilidade e permite novo check-in no mesmo quarto.
        logarComo("admin01");
        Hospedagem nova = checkInSemReserva(std, 101, 1, BEATRIZ);
        assertNotEquals(h.getCodigo(), nova.getCodigo());
    }

    @Test
    void semConfirmacaoNadaMudaETotalDivergenteEhRecusado() {
        Extrato e = checkOuts.conferir(h.getCodigo());
        consumos.lancar(h.getCodigo(), "AGUA", 1);   // total mudou depois da conferência
        assertThrows(NegocioException.class,
                () -> checkOuts.confirmarPagamento(h.getCodigo(), FormaPagamento.DINHEIRO, e.total()));
        assertThrows(NegocioException.class,
                () -> checkOuts.confirmarPagamento(h.getCodigo(), null, e.total().add(new BigDecimal("5.50"))));
        assertEquals(0, contar("SELECT COUNT(*) FROM pagamento"));
        assertEquals("OCUPADO", situacaoQuarto(101));
        assertEquals(1, contar("SELECT COUNT(*) FROM hospedagem WHERE situacao = 'ATIVA'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM conta_hospedagem WHERE situacao = 'ABERTA'"));
    }

    @Test
    void confirmacaoRepetidaEConsumoAposCheckOutSaoRecusados() {
        BigDecimal total = checkOuts.conferir(h.getCodigo()).total();
        checkOuts.confirmarPagamento(h.getCodigo(), FormaPagamento.PIX, total);
        assertThrows(NegocioException.class,
                () -> checkOuts.confirmarPagamento(h.getCodigo(), FormaPagamento.PIX, total));
        assertThrows(NegocioException.class, () -> consumos.lancar(h.getCodigo(), "AGUA", 1));
        assertThrows(NegocioException.class, () -> consumos.localizar(h.getCodigo()));
        assertThrows(NegocioException.class, () -> checkOuts.conferir(h.getCodigo()));
        assertEquals(1, contar("SELECT COUNT(*) FROM pagamento"));
        assertEquals(0, contar("SELECT COUNT(*) FROM consumo"));
    }

    @Test
    void falhaNaLiberacaoDoQuartoDesfazTodasAsAlteracoes() {
        BigDecimal total = checkOuts.conferir(h.getCodigo()).total();
        jdbc.execute("CREATE FUNCTION falhar() RETURNS trigger AS $$ BEGIN RAISE EXCEPTION 'falha simulada'; END $$ LANGUAGE plpgsql");
        jdbc.execute("CREATE TRIGGER falha_quarto BEFORE UPDATE ON quarto FOR EACH ROW EXECUTE FUNCTION falhar()");
        assertThrows(RuntimeException.class,
                () -> checkOuts.confirmarPagamento(h.getCodigo(), FormaPagamento.CARTAO_CREDITO, total));
        assertEquals(0, contar("SELECT COUNT(*) FROM pagamento"));
        assertEquals(1, contar("SELECT COUNT(*) FROM hospedagem WHERE situacao = 'ATIVA' AND data_hora_saida IS NULL"));
        assertEquals(1, contar("SELECT COUNT(*) FROM conta_hospedagem WHERE situacao = 'ABERTA' AND fechada_em IS NULL"));
        assertEquals("OCUPADO", situacaoQuarto(101));
    }

    /** Um lançamento que chega durante o check-out espera a trava e encontra a conta fechada. */
    @Test
    void lancamentoConcorrenteComCheckOutNaoEntraDepoisDoFechamento() throws Exception {
        BigDecimal total = checkOuts.conferir(h.getCodigo()).total();
        int recep = idFuncionario("recep01");
        AtomicReference<Throwable> erroDoLancamento = new AtomicReference<>();
        Thread[] lancamento = new Thread[1];
        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            servicoCheckOut.confirmar(h.getCodigo(), FormaPagamento.DINHEIRO, total, recep);
            lancamento[0] = new Thread(() -> {
                try {
                    servicoConsumo.lancar(h.getCodigo(), "AGUA", 1, recep);
                } catch (Throwable t) {
                    erroDoLancamento.set(t);
                }
            });
            lancamento[0].start();
            try {
                lancamento[0].join(1500);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            assertTrue(lancamento[0].isAlive(), "o lançamento deveria esperar o check-out terminar");
        });
        lancamento[0].join(10000);
        assertInstanceOf(NegocioException.class, erroDoLancamento.get());
        assertEquals(0, contar("SELECT COUNT(*) FROM consumo"));
        assertEquals(new BigDecimal("180.00"),
                jdbc.queryForObject("SELECT valor_total FROM pagamento", BigDecimal.class));
    }

    /** Duas estações confirmando ao mesmo tempo: só uma cobrança é registrada. */
    @Test
    void confirmacoesSimultaneasRegistramUmUnicoPagamento() throws Exception {
        BigDecimal total = checkOuts.conferir(h.getCodigo()).total();
        int recep = idFuncionario("recep01");
        int admin = idFuncionario("admin01");
        AtomicReference<Throwable> erro = new AtomicReference<>();
        Thread[] segunda = new Thread[1];
        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            servicoCheckOut.confirmar(h.getCodigo(), FormaPagamento.PIX, total, recep);
            segunda[0] = new Thread(() -> {
                try {
                    servicoCheckOut.confirmar(h.getCodigo(), FormaPagamento.DINHEIRO, total, admin);
                } catch (Throwable t) {
                    erro.set(t);
                }
            });
            segunda[0].start();
            try {
                segunda[0].join(1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
        segunda[0].join(10000);
        assertInstanceOf(NegocioException.class, erro.get());
        assertEquals(1, contar("SELECT COUNT(*) FROM pagamento WHERE forma = 'PIX'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM pagamento"));
    }
}
