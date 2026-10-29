package hospedabem.model;

import java.math.BigDecimal;

public record TipoQuarto(int id, String codigo, String descricao, int capacidadeMaxima, BigDecimal valorDiaria) {

    public TipoQuarto {
        if (codigo == null || codigo.isBlank()) throw new NegocioException("Código do tipo de quarto é obrigatório.");
        if (descricao == null || descricao.isBlank()) throw new NegocioException("Descrição é obrigatória.");
        if (capacidadeMaxima <= 0) throw new NegocioException("Capacidade máxima deve ser positiva.");
        if (valorDiaria == null || valorDiaria.signum() <= 0) throw new NegocioException("Valor da diária deve ser positivo.");
        codigo = codigo.trim().toUpperCase();
        descricao = descricao.trim();
    }

    @Override
    public String toString() {
        return codigo + " - " + descricao;
    }
}
