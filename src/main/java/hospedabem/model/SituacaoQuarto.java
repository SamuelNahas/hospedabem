package hospedabem.model;

public enum SituacaoQuarto {
    DISPONIVEL, OCUPADO, RESERVADO, MANUTENCAO;

    public boolean definivelNoCadastro() {
        return this == DISPONIVEL || this == MANUTENCAO;
    }
}
