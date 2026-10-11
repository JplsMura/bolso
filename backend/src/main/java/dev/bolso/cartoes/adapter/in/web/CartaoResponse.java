package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.application.CartaoComUso;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Dinheiro em texto ("4500.00"). {@code disponivel} pode ser negativo (passou do limite). */
public record CartaoResponse(
        @NotNull UUID id,
        @NotNull String nome,
        @NotNull Integer diaFechamento,
        @NotNull Integer diaVencimento,
        @NotNull String limite,
        @NotNull String usado,
        @NotNull String disponivel,
        @NotNull Long versao) {

    static CartaoResponse de(CartaoComUso c) {
        var cartao = c.cartao();
        return new CartaoResponse(
                cartao.id(),
                cartao.nome(),
                cartao.diaFechamento(),
                cartao.diaVencimento(),
                cartao.limite().toString(),
                c.usado().toString(),
                c.disponivel().toString(),
                cartao.versao());
    }
}
