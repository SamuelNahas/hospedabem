package hospedabem.model;

import java.time.LocalDate;

public class Reserva {
    private final int id;
    private final int codigo;
    private final Hospede responsavel;
    private final TipoQuarto tipo;
    private final Periodo periodo;
    private final int quantidadeHospedes;
    private final int funcionarioId;
    private final int versao;
    private SituacaoReserva situacao;

    public Reserva(int id, int codigo, Hospede responsavel, TipoQuarto tipo, Periodo periodo, int quantidadeHospedes,
                   SituacaoReserva situacao, int funcionarioId, int versao) {
        this.id = id;
        this.codigo = codigo;
        this.responsavel = responsavel;
        this.tipo = tipo;
        this.periodo = periodo;
        this.quantidadeHospedes = quantidadeHospedes;
        this.situacao = situacao;
        this.funcionarioId = funcionarioId;
        this.versao = versao;
    }

    public void exigirPermiteCheckIn(LocalDate hoje) {
        if (situacao != SituacaoReserva.CONFIRMADA)
            throw new NegocioException("Reserva " + codigo + " está " + situacao.name().toLowerCase()
                    + " e não permite check-in.");
        if (hoje.isBefore(periodo.entrada()))
            throw new NegocioException("A entrada prevista da reserva é " + periodo.entrada()
                    + ". Check-in antecipado exige alterar a reserva (UC09, iteração 2).");
        if (!hoje.isBefore(periodo.saida()))
            throw new NegocioException("O período da reserva já terminou; ela não permite check-in.");
    }

    public void converter(Hospedagem h) {
        situacao = SituacaoReserva.CONVERTIDA;
        h.setReserva(this);
    }

    public boolean mesmoEstadoDe(Reserva outra) {
        return outra != null && id == outra.id && versao == outra.versao && situacao == outra.situacao
                && tipo.id() == outra.tipo.id() && periodo.equals(outra.periodo)
                && quantidadeHospedes == outra.quantidadeHospedes && responsavel.id() == outra.responsavel.id();
    }

    public int getId() { return id; }
    public int getCodigo() { return codigo; }
    public Hospede getResponsavel() { return responsavel; }
    public TipoQuarto getTipo() { return tipo; }
    public Periodo getPeriodo() { return periodo; }
    public int getQuantidadeHospedes() { return quantidadeHospedes; }
    public SituacaoReserva getSituacao() { return situacao; }
    public int getFuncionarioId() { return funcionarioId; }
}
