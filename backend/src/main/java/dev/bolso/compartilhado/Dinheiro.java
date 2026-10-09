package dev.bolso.compartilhado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Valor em reais com exatamente 2 casas (o {@code numeric(14,2)} do banco). Nunca arredonda: se a
 * conta perderia centavos, lança exceção. Pode ser negativo (a sobra do mês, por exemplo); quem exige
 * valor positivo, como o lançamento, confere com {@link #ehPositivo()}.
 */
public record Dinheiro(BigDecimal valor) implements Comparable<Dinheiro> {

    // LIMITE vem antes de ZERO: o construtor usa LIMITE, e os campos estáticos iniciam na ordem em que aparecem
    private static final BigDecimal LIMITE = new BigDecimal("999999999999.99");
    private static final Pattern TEXTO_DA_API = Pattern.compile("\\d{1,12}\\.\\d{2}");

    public static final Dinheiro ZERO = new Dinheiro(new BigDecimal("0.00"));

    public Dinheiro {
        Objects.requireNonNull(valor, "valor");
        if (valor.scale() != 2) {
            throw new IllegalArgumentException("Dinheiro tem sempre 2 casas decimais: " + valor);
        }
        if (valor.abs().compareTo(LIMITE) > 0) {
            throw new IllegalArgumentException("Valor acima do limite de " + LIMITE.toPlainString());
        }
    }

    /** Lê o texto da API: só dígitos, ponto e 2 casas, sem sinal ("110.00"). */
    public static Dinheiro deTexto(String texto) {
        if (texto == null || !TEXTO_DA_API.matcher(texto).matches()) {
            throw new IllegalArgumentException("Valor inválido: use o formato 110.00");
        }
        return new Dinheiro(new BigDecimal(texto));
    }

    /** Vem do banco ({@code numeric(14,2)}); lança se perderia centavos. */
    public static Dinheiro deNumerico(BigDecimal numero) {
        return new Dinheiro(numero.setScale(2, RoundingMode.UNNECESSARY));
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(valor.subtract(outro.valor));
    }

    public boolean ehPositivo() {
        return valor.signum() > 0;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return valor.compareTo(outro.valor);
    }

    /** O texto da API ("110.00"). */
    @Override
    public String toString() {
        return valor.toPlainString();
    }
}
