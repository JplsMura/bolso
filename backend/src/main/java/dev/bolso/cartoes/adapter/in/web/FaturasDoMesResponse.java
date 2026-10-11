package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.application.FaturasDoMes;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FaturasDoMesResponse(@NotNull String mes, @NotNull List<FaturaDoCartaoResponse> faturas) {

    static FaturasDoMesResponse de(FaturasDoMes f) {
        return new FaturasDoMesResponse(f.mes().toString(), f.faturas().stream().map(FaturaDoCartaoResponse::de).toList());
    }
}
