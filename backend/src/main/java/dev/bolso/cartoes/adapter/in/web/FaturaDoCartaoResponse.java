package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.application.FaturaDoCartao;
import jakarta.validation.constraints.NotNull;

/** Um cartão e a fatura dele no mês. {@code fatura} não vem se o mês não teve compra nesse cartão. */
public record FaturaDoCartaoResponse(@NotNull CartaoResponse cartao, FaturaResponse fatura) {

    static FaturaDoCartaoResponse de(FaturaDoCartao f) {
        return new FaturaDoCartaoResponse(CartaoResponse.de(f.cartao()), FaturaResponse.de(f));
    }
}
