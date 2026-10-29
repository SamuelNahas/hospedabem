package hospedabem.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Check-in em andamento (UC10), da identificação da reserva ou do responsável até a hospedagem. */
public class Atendimento {
    private final Reserva reserva;
    private final Hospede responsavel;
    private final TipoQuarto tipo;
    private final Periodo periodo;
    private final List<Hospede> hospedes = new ArrayList<>();
    private Quarto quarto;

    private Atendimento(Reserva reserva, Hospede responsavel, TipoQuarto tipo, Periodo periodo) {
        this.reserva = reserva;
        this.responsavel = responsavel;
        this.tipo = tipo;
        this.periodo = periodo;
    }

    public static Atendimento comReserva(Reserva r, LocalDate hoje) {
        r.exigirPermiteCheckIn(hoje);
        return new Atendimento(r, r.getResponsavel(), r.getTipo(), new Periodo(hoje, r.getPeriodo().saida()));
    }

    public static Atendimento semReserva(Hospede responsavel, TipoQuarto tipo, Periodo periodo, LocalDate hoje) {
        if (!periodo.entrada().equals(hoje))
            throw new NegocioException("Período inválido: o check-in registra a entrada de hoje (" + hoje + ").");
        return new Atendimento(null, responsavel, tipo, periodo);
    }

    public void adicionar(Hospede h) {
        if (hospedes.stream().anyMatch(p -> p.id() == h.id()))
            throw new NegocioException("Hóspede já informado: " + h.nome() + ".");
        tipo.exigirComporta(hospedes.size() + 1);
        hospedes.add(h);
        quarto = null;
    }

    public void remover(Hospede h) {
        hospedes.removeIf(p -> p.id() == h.id());
        quarto = null;
    }

    public void exigirHospedes() {
        if (hospedes.isEmpty()) throw new NegocioException("Informe ao menos um hóspede.");
        if (hospedes.stream().noneMatch(p -> p.id() == responsavel.id()))
            throw new NegocioException("O hóspede responsável (" + responsavel.nome() + ") deve estar entre os participantes.");
    }

    public void selecionar(Quarto q) {
        exigirHospedes();
        q.exigirLivrePara(tipo);
        quarto = q;
    }

    public void exigirProntoParaConfirmar() {
        exigirHospedes();
        if (quarto == null) throw new NegocioException("Selecione o quarto antes de confirmar.");
    }

    public void exigirReservaInalterada(Reserva atual) {
        if (reserva == null) return;
        if (atual.getSituacao() != SituacaoReserva.CONFIRMADA)
            throw new NegocioException("A reserva foi " + atual.getSituacao().name().toLowerCase()
                    + " por outra operação após o resumo. O check-in foi encerrado sem alterações.");
        if (!reserva.mesmoEstadoDe(atual))
            throw new NegocioException("A reserva foi alterada após o resumo. Reinicie o check-in para revisar os dados.");
    }

    public Hospedagem criarHospedagem(Quarto quartoAtual, Reserva reservaAtual, LocalDateTime agora) {
        quartoAtual.exigirLivrePara(tipo);
        quartoAtual.getTipo().exigirComporta(hospedes.size());
        Hospedagem h = new Hospedagem(agora, periodo.saida());
        hospedes.forEach(h::adicionarHospede);
        h.setQuarto(quartoAtual);
        quartoAtual.ocupar();
        if (reservaAtual != null) reservaAtual.converter(h);
        return h;
    }

    public Reserva getReserva() { return reserva; }
    public Hospede getResponsavel() { return responsavel; }
    public TipoQuarto getTipo() { return tipo; }
    public Periodo getPeriodo() { return periodo; }
    public List<Hospede> getHospedes() { return Collections.unmodifiableList(hospedes); }
    public Quarto getQuarto() { return quarto; }
}
