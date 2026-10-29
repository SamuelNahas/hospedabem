package hospedabem.controller;

import hospedabem.dao.HospedagemDAO;
import hospedabem.dao.HospedeDAO;
import hospedabem.dao.QuartoDAO;
import hospedabem.dao.ReservaDAO;
import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.Atendimento;
import hospedabem.model.Hospedagem;
import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import hospedabem.model.Periodo;
import hospedabem.model.Quarto;
import hospedabem.model.Reserva;
import hospedabem.model.TipoQuarto;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ControladorCheckIn {
    private final Sessao sessao;
    private final ReservaDAO reservaDAO;
    private final HospedeDAO hospedeDAO;
    private final TipoQuartoDAO tipoDAO;
    private final QuartoDAO quartoDAO;
    private final HospedagemDAO hospedagemDAO;
    private final ServicoDisponibilidade disponibilidade;
    private final Clock relogio;
    private Atendimento atendimento;

    public ControladorCheckIn(Sessao sessao, ReservaDAO reservaDAO, HospedeDAO hospedeDAO, TipoQuartoDAO tipoDAO,
                              QuartoDAO quartoDAO, HospedagemDAO hospedagemDAO, ServicoDisponibilidade disponibilidade,
                              Clock relogio) {
        this.sessao = sessao;
        this.reservaDAO = reservaDAO;
        this.hospedeDAO = hospedeDAO;
        this.tipoDAO = tipoDAO;
        this.quartoDAO = quartoDAO;
        this.hospedagemDAO = hospedagemDAO;
        this.disponibilidade = disponibilidade;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Reserva iniciarCheckIn(int codReserva) {
        sessao.exigirQualquerPerfil();
        atendimento = null;
        Reserva r = reservaDAO.buscarPorCodigo(codReserva)
                .orElseThrow(() -> new NegocioException("Reserva não localizada: " + codReserva + "."));
        atendimento = Atendimento.comReserva(r, hoje());
        return r;
    }

    @Transactional(readOnly = true)
    public Atendimento iniciarCheckInSemReserva(String cpfResponsavel, String codTipo, LocalDate entrada, LocalDate saida) {
        sessao.exigirQualquerPerfil();
        atendimento = null;
        Hospede responsavel = buscarHospede(cpfResponsavel, "Hóspede responsável não cadastrado");
        Periodo periodo = new Periodo(entrada, saida);
        TipoQuarto tipo = tipoDAO.buscarPorCodigo(codTipo)
                .orElseThrow(() -> new NegocioException("Tipo de quarto não cadastrado: " + codTipo + "."));
        atendimento = Atendimento.semReserva(responsavel, tipo, periodo, hoje());
        return atendimento;
    }

    @Transactional(readOnly = true)
    public Hospede adicionarHospede(String cpfOuPassaporte) {
        sessao.exigirQualquerPerfil();
        Atendimento a = getAtendimento();
        Hospede h = buscarHospede(cpfOuPassaporte, "Hóspede não cadastrado");
        a.adicionar(h);
        return h;
    }

    public void removerHospede(Hospede h) {
        getAtendimento().remover(h);
    }

    @Transactional(readOnly = true)
    public List<Quarto> consultarQuartosDisponiveis() {
        sessao.exigirQualquerPerfil();
        Atendimento a = getAtendimento();
        a.exigirHospedes();
        disponibilidade.calcular(a.getTipo(), a.getPeriodo(), a.getReserva()).exigirVaga();
        List<Quarto> elegiveis = quartoDAO.listarElegiveis(a.getTipo().id());
        if (elegiveis.isEmpty())
            throw new NegocioException("Não há quarto do tipo " + a.getTipo().codigo() + " disponível no período.");
        return elegiveis;
    }

    @Transactional(readOnly = true)
    public Atendimento selecionarQuarto(int numero) {
        sessao.exigirQualquerPerfil();
        Atendimento a = getAtendimento();
        a.selecionar(quartoDAO.buscarPorNumero(numero)
                .orElseThrow(() -> new NegocioException("Quarto " + numero + " não cadastrado.")));
        return a;
    }

    @Transactional
    public Hospedagem confirmarCheckIn() {
        int funcionarioId = sessao.exigirQualquerPerfil().id();
        Atendimento a = getAtendimento();
        a.exigirProntoParaConfirmar();
        tipoDAO.bloquear(List.of(a.getTipo().id()));
        Reserva reservaAtual = a.getReserva() == null ? null
                : reservaDAO.buscarPorCodigoParaAtualizar(a.getReserva().getCodigo())
                .orElseThrow(() -> new NegocioException("A reserva foi removida por outra operação."));
        a.exigirReservaInalterada(reservaAtual);
        int numero = a.getQuarto().getNumero();
        Quarto quartoAtual = quartoDAO.buscarPorNumeroParaAtualizar(numero).orElseThrow(() -> Quarto.indisponivel(numero));
        Hospedagem h = a.criarHospedagem(quartoAtual, reservaAtual, agora());
        disponibilidade.calcular(a.getTipo(), a.getPeriodo(), a.getReserva()).exigirVaga();
        try {
            hospedagemDAO.salvar(h, funcionarioId);
        } catch (DuplicateKeyException e) {
            throw Quarto.indisponivel(numero);
        }
        quartoDAO.atualizar(quartoAtual);
        if (reservaAtual != null) reservaDAO.atualizar(reservaAtual);
        atendimento = null;
        return h;
    }

    public Atendimento getAtendimento() {
        if (atendimento == null) throw new NegocioException("Inicie o check-in com uma reserva ou sem reserva.");
        return atendimento;
    }

    private Hospede buscarHospede(String documento, String motivo) {
        return hospedeDAO.buscarPorDocumento(documento).orElseThrow(() -> new NegocioException(motivo + ": " + documento
                + ". O cadastro de hóspedes (UC06) será entregue na iteração 2; regularize o cadastro e reinicie o check-in."));
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
    }
}
