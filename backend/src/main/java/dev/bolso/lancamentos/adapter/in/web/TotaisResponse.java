package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.domain.TotaisDoMes;
import jakarta.validation.constraints.NotNull;

/** Todos em texto ("4890.00"); a sobra pode ser negativa. */
public record TotaisResponse(@NotNull String entradas, @NotNull String saidas, @NotNull String sobra) {

    static TotaisResponse de(TotaisDoMes totais) {
        return new TotaisResponse(totais.entradas().toString(), totais.saidas().toString(), totais.sobra().toString());
    }
}
