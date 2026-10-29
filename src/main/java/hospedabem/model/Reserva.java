package hospedabem.model;

/** Reserva de um tipo de quarto; o quarto físico só é definido no check-in. */
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

    public void converter(Hospedagem h) {
        situacao = SituacaoReserva.CONVERTIDA;
        h.setReserva(this);
    }

    /** Detecta alteração por outra transação entre o resumo e a confirmação do check-in. */
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
