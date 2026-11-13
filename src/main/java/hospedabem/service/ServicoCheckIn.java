package hospedabem.service;

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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ServicoCheckIn {
    private final ReservaDAO reservaDAO;
    private final HospedeDAO hospedeDAO;
    private final TipoQuartoDAO tipoDAO;
    private final QuartoDAO quartoDAO;
    private final HospedagemDAO hospedagemDAO;
    private final ServicoDisponibilidade disponibilidade;
    private final Clock relogio;

    public ServicoCheckIn(ReservaDAO reservaDAO, HospedeDAO hospedeDAO, TipoQuartoDAO tipoDAO, QuartoDAO quartoDAO,
                          HospedagemDAO hospedagemDAO, ServicoDisponibilidade disponibilidade, Clock relogio) {
        this.reservaDAO = reservaDAO;
        this.hospedeDAO = hospedeDAO;
        this.tipoDAO = tipoDAO;
        this.quartoDAO = quartoDAO;
        this.hospedagemDAO = hospedagemDAO;
        this.disponibilidade = disponibilidade;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Atendimento iniciarComReserva(int codReserva) {
        Reserva r = reservaDAO.buscarPorCodigo(codReserva)
                .orElseThrow(() -> new NegocioException("Reserva não localizada: " + codReserva + "."));
        return Atendimento.comReserva(r, hoje());
    }

    @Transactional(readOnly = true)
    public Atendimento iniciarSemReserva(String cpfResponsavel, String codTipo, Periodo periodo) {
        Hospede responsavel = buscarHospede(cpfResponsavel, "Hóspede responsável não cadastrado");
        TipoQuarto tipo = tipoDAO.buscarPorCodigo(codTipo)
                .orElseThrow(() -> new NegocioException("Tipo de quarto não cadastrado: " + codTipo + "."));
        return Atendimento.semReserva(responsavel, tipo, periodo, hoje());
    }

    @Transactional(readOnly = true)
    public Hospede adicionarHospede(Atendimento a, String cpfOuPassaporte) {
        Hospede h = buscarHospede(cpfOuPassaporte, "Hóspede não cadastrado");
        a.adicionar(h);
        return h;
    }

    @Transactional(readOnly = true)
    public List<Quarto> consultarQuartosDisponiveis(Atendimento a) {
        a.exigirHospedes();
        disponibilidade.calcular(a.getTipo(), a.getPeriodo(), a.getReserva()).exigirVaga();
        List<Quarto> elegiveis = quartoDAO.listarElegiveis(a.getTipo().id());
        if (elegiveis.isEmpty())
            throw new NegocioException("Não há quarto do tipo " + a.getTipo().codigo() + " disponível no período.");
        return elegiveis;
    }

    @Transactional(readOnly = true)
    public void selecionarQuarto(Atendimento a, int numero) {
        a.selecionar(quartoDAO.buscarPorNumero(numero)
                .orElseThrow(() -> new NegocioException("Quarto " + numero + " não cadastrado.")));
    }

    @Transactional
    public Hospedagem confirmar(Atendimento a, int funcionarioId) {
        a.exigirProntoParaConfirmar();
        tipoDAO.bloquear(List.of(a.getTipo().id()));
        Reserva reservaAtual = a.getReserva() == null ? null
                : reservaDAO.buscarPorCodigoParaAtualizar(a.getReserva().getCodigo())
                .orElseThrow(() -> new NegocioException("A reserva foi removida por outra operação."));
        a.exigirReservaInalterada(reservaAtual);
        List<Integer> hospedes = a.getHospedes().stream().map(Hospede::id).toList();
        if (hospedeDAO.bloquear(hospedes) < hospedes.stream().distinct().count())
            throw new NegocioException("Um dos hóspedes foi excluído por outra estação. Reinicie o check-in.");
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
        return h;
    }

    private Hospede buscarHospede(String documento, String motivo) {
        return hospedeDAO.buscarPorDocumento(documento).orElseThrow(() -> new NegocioException(motivo + ": " + documento
                + ". Regularize o cadastro em \"Hóspedes\" (UC06) e reinicie o check-in."));
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
    }
}
