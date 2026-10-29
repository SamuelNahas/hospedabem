package hospedabem.controller;

import hospedabem.dao.HospedeDAO;
import hospedabem.dao.ReservaDAO;
import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import hospedabem.model.Periodo;
import hospedabem.model.Reserva;
import hospedabem.model.SolicitacaoReserva;
import hospedabem.model.TipoQuarto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class ControladorReserva {
    private final Sessao sessao;
    private final HospedeDAO hospedeDAO;
    private final TipoQuartoDAO tipoDAO;
    private final ReservaDAO reservaDAO;
    private final ServicoDisponibilidade disponibilidade;
    private final Clock relogio;

    public ControladorReserva(Sessao sessao, HospedeDAO hospedeDAO, TipoQuartoDAO tipoDAO, ReservaDAO reservaDAO,
                              ServicoDisponibilidade disponibilidade, Clock relogio) {
        this.sessao = sessao;
        this.hospedeDAO = hospedeDAO;
        this.tipoDAO = tipoDAO;
        this.reservaDAO = reservaDAO;
        this.disponibilidade = disponibilidade;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Hospede buscarResponsavel(String cpfOuPassaporte) {
        sessao.exigirQualquerPerfil();
        return hospedeDAO.buscarPorDocumento(cpfOuPassaporte).orElseThrow(() -> new NegocioException(
                "Hóspede não cadastrado: " + cpfOuPassaporte + ". O cadastro de hóspedes (UC06) será entregue na "
                        + "iteração 2; nesta versão use os hóspedes da carga inicial."));
    }

    @Transactional
    public Reserva registrar(String cpfResponsavel, String codigoTipo, LocalDate entrada, LocalDate saida, int quantidade) {
        int funcionarioId = sessao.exigirQualquerPerfil().id();
        TipoQuarto tipo = tipoDAO.buscarPorCodigo(codigoTipo)
                .orElseThrow(() -> new NegocioException("Tipo de quarto não cadastrado: " + codigoTipo));
        var solicitacao = new SolicitacaoReserva(buscarResponsavel(cpfResponsavel), tipo, new Periodo(entrada, saida), quantidade);
        solicitacao.validar(LocalDate.now(relogio));
        tipoDAO.bloquear(List.of(tipo.id()));
        disponibilidade.calcular(tipo, solicitacao.periodo()).exigirVaga();
        return reservaDAO.buscarPorCodigo(reservaDAO.inserir(solicitacao, funcionarioId)).orElseThrow();
    }
}
