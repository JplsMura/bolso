package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.domain.Parcela;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** "2/6": parcela {@code numero} de {@code total}. */
public record ParcelaResponse(@NotNull UUID parcelamentoId, @NotNull Integer numero, @NotNull Integer total) {

    static ParcelaResponse de(Parcela parcela) {
        return parcela == null ? null : new ParcelaResponse(parcela.parcelamentoId(), parcela.numero(), parcela.total());
    }
}
