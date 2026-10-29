package hospedabem.model;

public class Quarto {
    private final int id;
    private final int numero;
    private final int andar;
    private final TipoQuarto tipo;
    private SituacaoQuarto situacao;

    public Quarto(int id, int numero, int andar, TipoQuarto tipo, SituacaoQuarto situacao) {
        if (numero <= 0) throw new NegocioException("Número do quarto deve ser positivo.");
        if (tipo == null) throw new NegocioException("Tipo de quarto é obrigatório.");
        if (situacao == null) throw new NegocioException("Situação é obrigatória.");
        this.id = id;
        this.numero = numero;
        this.andar = andar;
        this.tipo = tipo;
        this.situacao = situacao;
    }

    public void ocupar() {
        situacao = SituacaoQuarto.OCUPADO;
    }

    public int getId() { return id; }
    public int getNumero() { return numero; }
    public int getAndar() { return andar; }
    public TipoQuarto getTipo() { return tipo; }
    public SituacaoQuarto getSituacao() { return situacao; }

    @Override
    public String toString() {
        return "Quarto " + numero;
    }
}
