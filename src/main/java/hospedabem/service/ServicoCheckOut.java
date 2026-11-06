package hospedabem.service;

import hospedabem.dao.HospedagemDAO;
import hospedabem.dao.PagamentoDAO;
import hospedabem.dao.QuartoDAO;
import hospedabem.model.Extrato;
import hospedabem.model.FormaPagamento;
import hospedabem.model.Hospedagem;
import hospedabem.model.Pagamento;
import hospedabem.model.ResumoHospedagem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** UC12 Realizar Check-out. Nada é gravado antes da confirmação do pagamento. */
@Service
public class ServicoCheckOut {
    private final HospedagemDAO hospedagemDAO;
    private final PagamentoDAO pagamentoDAO;
    private final QuartoDAO quartoDAO;
    private final Clock relogio;

    public ServicoCheckOut(HospedagemDAO hospedagemDAO, PagamentoDAO pagamentoDAO, QuartoDAO quartoDAO, Clock relogio) {
        this.hospedagemDAO = hospedagemDAO;
        this.pagamentoDAO = pagamentoDAO;
        this.quartoDAO = quartoDAO;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<ResumoHospedagem> listarHospedagensAtivas() {
        return hospedagemDAO.listarAtivas();
    }

    /** Extrato para conferência: diárias até agora e consumos lançados. */
    @Transactional(readOnly = true)
    public Extrato conferir(int codigoHospedagem) {
        Hospedagem h = hospedagemDAO.buscarPorCodigo(codigoHospedagem)
                .orElseThrow(() -> Hospedagem.naoLocalizada(codigoHospedagem));
        return h.extrato(agora());
    }

    /**
     * Confirma o pagamento e conclui o check-out numa única transação: grava o pagamento, encerra a
     * hospedagem com a data/hora de saída, fecha a conta e libera o quarto. Trava a hospedagem, a
     * conta e o quarto primeiro, então um lançamento de consumo simultâneo espera e depois encontra
     * a conta fechada, e uma segunda confirmação encontra a hospedagem encerrada. O UNIQUE de
     * {@code pagamento.conta_hospedagem_id} fica como última barreira no banco.
     */
    @Transactional
    public Pagamento confirmar(int codigoHospedagem, FormaPagamento forma, BigDecimal totalConferido, int funcionarioId) {
        Hospedagem h = hospedagemDAO.buscarPorCodigoParaAtualizar(codigoHospedagem)
                .orElseThrow(() -> Hospedagem.naoLocalizada(codigoHospedagem));
        Pagamento p = h.encerrar(forma, totalConferido, agora());
        pagamentoDAO.inserir(h.getConta().getId(), p, funcionarioId);
        hospedagemDAO.encerrar(h);
        quartoDAO.atualizar(h.getQuarto());
        return p;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
    }
}
