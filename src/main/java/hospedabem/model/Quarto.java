package hospedabem.model;

public class Quarto {
    private final int id;
    private final int numero;
    private final int andar;
    private final TipoQuarto tipo;
    private final boolean hospedado;
    private SituacaoQuarto situacao;

    public Quarto(int id, int numero, int andar, TipoQuarto tipo, SituacaoQuarto situacao) {
        this(id, numero, andar, tipo, situacao, false);
    }

    public Quarto(int id, int numero, int andar, TipoQuarto tipo, SituacaoQuarto situacao, boolean hospedado) {
        if (numero <= 0) throw new NegocioException("Número do quarto deve ser positivo.");
        if (tipo == null) throw new NegocioException("Tipo de quarto é obrigatório.");
        if (situacao == null) throw new NegocioException("Situação é obrigatória.");
        this.id = id;
        this.numero = numero;
        this.andar = andar;
        this.tipo = tipo;
        this.situacao = situacao;
        this.hospedado = hospedado;
    }

    public static NegocioException indisponivel(int numero) {
        return new NegocioException("Quarto " + numero + " indisponível (ocupado ou em manutenção). "
                + "Consulte os quartos novamente.");
    }

    public void ocupar() {
        situacao = SituacaoQuarto.OCUPADO;
    }

    public void exigirSituacaoDeCadastro() {
        if (!situacao.definivelNoCadastro())
            throw new NegocioException("Situação " + situacao + " não pode ser definida pelo cadastro: ela resulta do "
                    + "check-in ou da reserva. Use DISPONIVEL ou MANUTENCAO.");
    }

    public void exigirAlteracaoPara(Quarto novo) {
        if (!hospedado) {
            novo.exigirSituacaoDeCadastro();
            return;
        }
        if (novo.tipo.id() != tipo.id() || novo.situacao != SituacaoQuarto.OCUPADO)
            throw new NegocioException("Quarto " + numero + " tem hospedagem ativa: não pode ser liberado, "
                    + "reclassificado ou colocado em manutenção pelo cadastro. A liberação ocorre no check-out.");
    }

    public void exigirLivrePara(TipoQuarto tipoEsperado) {
        if (tipo.id() != tipoEsperado.id())
            throw new NegocioException("Quarto " + numero + " não é do tipo " + tipoEsperado.codigo() + ".");
        if (hospedado || situacao == SituacaoQuarto.MANUTENCAO) throw indisponivel(numero);
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
