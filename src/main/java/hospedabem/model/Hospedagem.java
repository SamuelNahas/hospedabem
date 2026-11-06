package hospedabem.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hospedagem {
    private int id;
    private int codigo;
    private final LocalDateTime dataHoraEntrada;
    private final LocalDate dataSaidaPrevista;
    private SituacaoHospedagem situacao = SituacaoHospedagem.ATIVA;
    private LocalDateTime dataHoraSaida;
    private final List<Hospede> hospedes = new ArrayList<>();
    private Quarto quarto;
    private Reserva reserva;
    private ContaDaHospedagem conta;

    public Hospedagem(LocalDateTime dataHoraEntrada, LocalDate dataSaidaPrevista) {
        if (!dataSaidaPrevista.isAfter(dataHoraEntrada.toLocalDate()))
            throw new NegocioException("A saída prevista deve ser posterior à entrada.");
        this.dataHoraEntrada = dataHoraEntrada;
        this.dataSaidaPrevista = dataSaidaPrevista;
    }

    /** Reconstitui uma hospedagem gravada, com o quarto (opcional) e a conta. */
    public static Hospedagem reconstituir(int id, int codigo, LocalDateTime entrada, LocalDate saidaPrevista,
                                          SituacaoHospedagem situacao, LocalDateTime saida, Quarto quarto,
                                          ContaDaHospedagem conta) {
        Hospedagem h = new Hospedagem(entrada, saidaPrevista);
        h.registrarIdentificacao(id, codigo);
        h.situacao = situacao;
        h.dataHoraSaida = saida;
        h.quarto = quarto;
        h.conta = conta;
        return h;
    }

    public void adicionarHospede(Hospede h) {
        hospedes.add(h);
    }

    public void setQuarto(Quarto q) {
        quarto = q;
        conta = new ContaDaHospedagem(q.getTipo().valorDiaria());
    }

    void setReserva(Reserva r) {
        reserva = r;
    }

    public void registrarIdentificacao(int id, int codigo) {
        this.id = id;
        this.codigo = codigo;
    }

    public void exigirAtiva() {
        if (situacao != SituacaoHospedagem.ATIVA)
            throw new NegocioException("A hospedagem " + codigo + " está encerrada.");
    }

    /** UC11: lançamento aceito somente em hospedagem ATIVA com conta ABERTA. */
    public Consumo lancarConsumo(ItemConsumo item, int quantidade, LocalDateTime agora) {
        exigirAtiva();
        return conta.lancar(item, quantidade, agora);
    }

    public int getId() { return id; }
    public int getCodigo() { return codigo; }
    public LocalDateTime getDataHoraEntrada() { return dataHoraEntrada; }
    public LocalDate getDataSaidaPrevista() { return dataSaidaPrevista; }
    public SituacaoHospedagem getSituacao() { return situacao; }
    public LocalDateTime getDataHoraSaida() { return dataHoraSaida; }
    public List<Hospede> getHospedes() { return Collections.unmodifiableList(hospedes); }
    public Quarto getQuarto() { return quarto; }
    public Reserva getReserva() { return reserva; }
    public ContaDaHospedagem getConta() { return conta; }
}
