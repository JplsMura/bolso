package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.application.PrevisaoDaFatura;
import dev.bolso.cartoes.domain.StatusDaFatura;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Em qual fatura cai a compra. {@code status} não vem se a fatura ainda não existe. */
public record FaturaDaCompraResponse(
        @NotNull String mes, @NotNull LocalDate fechamento, @NotNull LocalDate vencimento, StatusDaFatura status) {

    static FaturaDaCompraResponse de(PrevisaoDaFatura p) {
        return new FaturaDaCompraResponse(p.mes().toString(), p.fechamento(), p.vencimento(), p.status());
    }
}
