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
    private final SituacaoHospedagem situacao = SituacaoHospedagem.ATIVA;
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

    public void adicionarHospede(Hospede h) {
        hospedes.add(h);
    }

    /** Associa o quarto e abre a conta com a diária do tipo (DC 1.2.1 e 1.4). */
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

    public int getId() { return id; }
    public int getCodigo() { return codigo; }
    public LocalDateTime getDataHoraEntrada() { return dataHoraEntrada; }
    public LocalDate getDataSaidaPrevista() { return dataSaidaPrevista; }
    public SituacaoHospedagem getSituacao() { return situacao; }
    public List<Hospede> getHospedes() { return Collections.unmodifiableList(hospedes); }
    public Quarto getQuarto() { return quarto; }
    public Reserva getReserva() { return reserva; }
    public ContaDaHospedagem getConta() { return conta; }
}
