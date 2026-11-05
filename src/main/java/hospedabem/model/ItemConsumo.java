package hospedabem.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public record ItemConsumo(int id, String codigo, String descricao, String categoria, BigDecimal preco) {
    public ItemConsumo {
        codigo = texto(codigo == null ? null : codigo.toUpperCase(Locale.ROOT), "Código", 20);
        descricao = texto(descricao, "Descrição", 120);
        categoria = texto(categoria, "Categoria", 80);
        if (preco == null || preco.signum() <= 0)
            throw new NegocioException("Preço deve ser positivo.");
        if (preco.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new NegocioException("Preço não pode ultrapassar R$ 99.999.999,99.");
        try {
            preco = preco.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new NegocioException("Preço deve ter no máximo duas casas decimais.");
        }
    }

    private static String texto(String valor, String nome, int limite) {
        if (valor == null || valor.isBlank()) throw new NegocioException(nome + " é obrigatório(a).");
        String normalizado = valor.trim();
        if (normalizado.length() > limite)
            throw new NegocioException(nome + " deve ter no máximo " + limite + " caracteres.");
        return normalizado;
    }

    @Override
    public String toString() {
        return codigo + " - " + descricao;
    }
}
