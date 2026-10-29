package hospedabem.controller;

import hospedabem.dao.HospedagemDAO;
import hospedabem.dao.HospedeDAO;
import hospedabem.dao.QuartoDAO;
import hospedabem.dao.ReservaDAO;
import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.Hospedagem;
import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import hospedabem.model.Periodo;
import hospedabem.model.Quarto;
import hospedabem.model.Reserva;
import hospedabem.model.SituacaoQuarto;
import hospedabem.model.SituacaoReserva;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UC10 Realizar Check-in. Mantém o estado de um atendimento (um objeto por tela) e segue os eventos do DSS.
 * confirmarCheckIn() implementa o contrato numa única transação.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ControladorCheckIn {

    /** Dados apresentados antes da confirmação (passo 8 do fluxo principal). */
    public record Resumo(Reserva reserva, Hospede responsavel, List<Hospede> hospedes, Quarto quarto, Periodo periodo) {
    }

    private final Sessao sessao;
    private final ReservaDAO reservaDAO;
    private final HospedeDAO hospedeDAO;
    private final TipoQuartoDAO tipoDAO;
    private final QuartoDAO quartoDAO;
    private final HospedagemDAO hospedagemDAO;
    private final ServicoDisponibilidade disponibilidade;
    private final Clock relogio;

    private Reserva reserva;
    private Hospede responsavel;
    private TipoQuarto tipo;
    private Periodo periodo;
    private final List<Hospede> participantes = new ArrayList<>();
    private Quarto quartoSelecionado;

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

    /** DSS 1.1. Fluxos 1a (não localizada) e 1b (cancelada ou convertida). */
    @Transactional(readOnly = true)
    public Reserva iniciarCheckIn(int codReserva) {
        sessao.exigirQualquerPerfil();
        limpar();
        Reserva r = reservaDAO.buscarPorCodigo(codReserva)
                .orElseThrow(() -> new NegocioException("Reserva não localizada: " + codReserva + "."));
        exigirReservaUtilizavel(r);
        reserva = r;
        responsavel = r.getResponsavel();
        tipo = r.getTipo();
        periodo = new Periodo(hoje(), r.getPeriodo().saida());
        return r;
    }

    /** DSS 2.1. Variante V1: não cria reserva intermediária. */
    @Transactional(readOnly = true)
    public Hospede iniciarCheckInSemReserva(String cpfResponsavel, String codTipo, LocalDate dataEntrada,
                                            LocalDate dataSaida) {
        sessao.exigirQualquerPerfil();
        limpar();
        Hospede h = hospedeDAO.buscarPorDocumento(cpfResponsavel)
                .orElseThrow(() -> naoCadastrado("Hóspede responsável não cadastrado: " + cpfResponsavel + "."));
        Periodo p = new Periodo(dataEntrada, dataSaida);
        if (!dataEntrada.equals(hoje()))
            throw new NegocioException("Período inválido: o check-in registra a entrada de hoje (" + hoje() + ").");
        TipoQuarto t = tipoDAO.buscarPorCodigo(codTipo)
                .orElseThrow(() -> new NegocioException("Tipo de quarto não cadastrado: " + codTipo + "."));
        responsavel = h;
        tipo = t;
        periodo = p;
        return h;
    }

    /** DSS 3.1, chamado para cada participante, inclusive o responsável. */
    @Transactional(readOnly = true)
    public Hospede adicionarHospede(String cpfOuPassaporte) {
        sessao.exigirQualquerPerfil();
        exigirIniciado();
        Hospede h = hospedeDAO.buscarPorDocumento(cpfOuPassaporte)
                .orElseThrow(() -> naoCadastrado("Hóspede não cadastrado: " + cpfOuPassaporte + "."));
        if (participantes.stream().anyMatch(p -> p.id() == h.id()))
            throw new NegocioException("Hóspede já informado: " + h.nome() + ".");
        if (participantes.size() + 1 > tipo.capacidadeMaxima())
            throw new NegocioException("Capacidade do quarto excedida: o tipo " + tipo.codigo() + " comporta "
                    + tipo.capacidadeMaxima() + " hóspede(s).");
        participantes.add(h);
        quartoSelecionado = null;
        return h;
    }

    public void removerHospede(Hospede h) {
        participantes.removeIf(p -> p.id() == h.id());
        quartoSelecionado = null;
    }

    /** DSS 4.1. Fluxo 5a: nenhum quarto compatível. */
    @Transactional(readOnly = true)
    public List<Quarto> consultarQuartosDisponiveis() {
        sessao.exigirQualquerPerfil();
        exigirParticipantes();
        if (disponibilidade.calcular(tipo, periodo, idReserva()).livres() < 1) throw semQuarto();
        List<Quarto> elegiveis = quartoDAO.listarElegiveis(tipo.id());
        if (elegiveis.isEmpty()) throw semQuarto();
        return elegiveis;
    }

    /** DSS 5.1. Fluxo 7a: quarto indisponível. */
    @Transactional(readOnly = true)
    public Resumo selecionarQuarto(int numero) {
        sessao.exigirQualquerPerfil();
        exigirParticipantes();
        Quarto q = quartoDAO.buscarPorNumero(numero)
                .orElseThrow(() -> new NegocioException("Quarto " + numero + " não cadastrado."));
        exigirQuartoLivre(q);
        quartoSelecionado = q;
        return new Resumo(reserva, responsavel, getParticipantes(), q, periodo);
    }

    /**
     * DSS 6.1 / contrato confirmarCheckIn(): revalida reserva, quarto, capacidade e disponibilidade com bloqueios
     * e grava hospedagem, participantes, conta, ocupação e conversão na mesma transação. Fluxo 9a em caso de conflito.
     */
    @Transactional
    public Hospedagem confirmarCheckIn() {
        int funcionarioId = sessao.exigirQualquerPerfil().id();
        exigirParticipantes();
        if (quartoSelecionado == null) throw new NegocioException("Selecione o quarto antes de confirmar.");
        tipoDAO.bloquear(List.of(tipo.id()));

        Reserva r = null;
        if (reserva != null) {
            r = reservaDAO.buscarPorCodigoParaAtualizar(reserva.getCodigo())
                    .orElseThrow(() -> new NegocioException("A reserva foi removida por outra operação."));
            if (r.getSituacao() != SituacaoReserva.CONFIRMADA)
                throw new NegocioException("A reserva foi " + r.getSituacao().name().toLowerCase()
                        + " por outra operação após o resumo. O check-in foi encerrado sem alterações.");
            if (!reserva.mesmoEstadoDe(r))
                throw new NegocioException("A reserva foi alterada após o resumo. Reinicie o check-in para revisar os dados.");
        }
        Quarto q = quartoDAO.buscarPorNumeroParaAtualizar(quartoSelecionado.getNumero())
                .orElseThrow(() -> quartoIndisponivel(quartoSelecionado.getNumero()));
        exigirQuartoLivre(q);
        if (participantes.size() > q.getTipo().capacidadeMaxima())
            throw new NegocioException("Capacidade do quarto excedida após alteração do tipo.");
        if (disponibilidade.calcular(tipo, periodo, idReserva()).livres() < 1)
            throw new NegocioException("O tipo " + tipo.codigo() + " ficou sem disponibilidade no período. "
                    + "Consulte os quartos novamente.");

        Hospedagem h = new Hospedagem(agora(), periodo.saida());
        participantes.forEach(h::adicionarHospede);
        h.setQuarto(q);
        q.ocupar();
        if (r != null) r.converter(h);
        try {
            hospedagemDAO.salvar(h, funcionarioId);
        } catch (DuplicateKeyException e) {
            throw quartoIndisponivel(q.getNumero());
        }
        quartoDAO.atualizar(q);
        if (r != null) reservaDAO.atualizar(r);
        limpar();
        return h;
    }

    public Reserva getReserva() { return reserva; }
    public Hospede getResponsavel() { return responsavel; }
    public TipoQuarto getTipo() { return tipo; }
    public Periodo getPeriodo() { return periodo; }
    public List<Hospede> getParticipantes() { return Collections.unmodifiableList(new ArrayList<>(participantes)); }

    private void exigirReservaUtilizavel(Reserva r) {
        if (r.getSituacao() != SituacaoReserva.CONFIRMADA)
            throw new NegocioException("Reserva " + r.getCodigo() + " está " + r.getSituacao().name().toLowerCase()
                    + " e não permite check-in.");
        LocalDate hoje = hoje();
        if (hoje.isBefore(r.getPeriodo().entrada()))
            throw new NegocioException("A entrada prevista da reserva é " + r.getPeriodo().entrada()
                    + ". Check-in antecipado exige alterar a reserva (UC09, iteração 2).");
        if (!hoje.isBefore(r.getPeriodo().saida()))
            throw new NegocioException("O período da reserva já terminou; ela não permite check-in.");
    }

    private void exigirQuartoLivre(Quarto q) {
        if (q.getTipo().id() != tipo.id())
            throw new NegocioException("Quarto " + q.getNumero() + " não é do tipo " + tipo.codigo() + ".");
        if (q.getSituacao() == SituacaoQuarto.MANUTENCAO || quartoDAO.possuiHospedagem(q.getId(), true))
            throw quartoIndisponivel(q.getNumero());
    }

    private void exigirIniciado() {
        if (tipo == null) throw new NegocioException("Inicie o check-in com uma reserva ou sem reserva.");
    }

    private void exigirParticipantes() {
        exigirIniciado();
        if (participantes.isEmpty()) throw new NegocioException("Informe ao menos um hóspede.");
        if (participantes.stream().noneMatch(p -> p.id() == responsavel.id()))
            throw new NegocioException("O hóspede responsável (" + responsavel.nome() + ") deve estar entre os participantes.");
    }

    private int idReserva() {
        return reserva == null ? ServicoDisponibilidade.NENHUMA_RESERVA : reserva.getId();
    }

    private void limpar() {
        reserva = null;
        responsavel = null;
        tipo = null;
        periodo = null;
        participantes.clear();
        quartoSelecionado = null;
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
    }

    private static NegocioException naoCadastrado(String mensagem) {
        return new NegocioException(mensagem + " O cadastro de hóspedes (UC06) será entregue na iteração 2; "
                + "regularize o cadastro e reinicie o check-in.");
    }

    private static NegocioException quartoIndisponivel(int numero) {
        return new NegocioException("Quarto " + numero + " indisponível (ocupado ou em manutenção). "
                + "Consulte os quartos novamente.");
    }

    private NegocioException semQuarto() {
        return new NegocioException("Nenhum quarto do tipo " + tipo.codigo() + " disponível para o período.");
    }
}
