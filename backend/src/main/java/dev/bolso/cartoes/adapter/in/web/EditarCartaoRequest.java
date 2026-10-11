package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.domain.DadosDoCartao;
import dev.bolso.compartilhado.Dinheiro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Corpo do PUT: os campos do POST mais a {@code versao} que a tela leu. */
public record EditarCartaoRequest(
        @NotNull String nome,
        @NotNull Integer diaFechamento,
        @NotNull Integer diaVencimento,
        @NotNull @Pattern(regexp = "\\d{1,12}\\.\\d{2}", message = "use o formato 4500.00") String limite,
        @NotNull Long versao) {

    DadosDoCartao paraDados() {
        return new DadosDoCartao(nome, diaFechamento, diaVencimento, Dinheiro.deTexto(limite));
    }
}
