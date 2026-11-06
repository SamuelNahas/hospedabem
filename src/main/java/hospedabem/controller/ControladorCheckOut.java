package hospedabem.controller;

import hospedabem.model.Extrato;
import hospedabem.model.FormaPagamento;
import hospedabem.model.Pagamento;
import hospedabem.model.ResumoHospedagem;
import hospedabem.service.ServicoCheckOut;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** UC12 Realizar Check-out: recepcionista e administrador (RNF-10). */
@Component
public class ControladorCheckOut {
    private final Sessao sessao;
    private final ServicoCheckOut servico;

    public ControladorCheckOut(Sessao sessao, ServicoCheckOut servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public List<ResumoHospedagem> listarHospedagensAtivas() {
        sessao.exigirQualquerPerfil();
        return servico.listarHospedagensAtivas();
    }

    public Extrato conferir(int codigoHospedagem) {
        sessao.exigirQualquerPerfil();
        return servico.conferir(codigoHospedagem);
    }

    public Pagamento confirmarPagamento(int codigoHospedagem, FormaPagamento forma, BigDecimal totalConferido) {
        int funcionario = sessao.exigirQualquerPerfil().id();
        return servico.confirmar(codigoHospedagem, forma, totalConferido, funcionario);
    }
}
