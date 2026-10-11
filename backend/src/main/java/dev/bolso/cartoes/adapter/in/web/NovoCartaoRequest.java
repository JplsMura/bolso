package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.domain.DadosDoCartao;
import dev.bolso.compartilhado.Dinheiro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Corpo do POST. Formato do limite: 400; dias e nome fora da regra: 422. */
public record NovoCartaoRequest(
        @NotNull String nome,
        @NotNull Integer diaFechamento,
        @NotNull Integer diaVencimento,
        @NotNull @Pattern(regexp = "\\d{1,12}\\.\\d{2}", message = "use o formato 4500.00") String limite) {

    DadosDoCartao paraDados() {
        return new DadosDoCartao(nome, diaFechamento, diaVencimento, Dinheiro.deTexto(limite));
    }
}
