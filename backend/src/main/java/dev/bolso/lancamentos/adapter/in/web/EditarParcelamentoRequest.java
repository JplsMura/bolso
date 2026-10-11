package dev.bolso.lancamentos.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Corpo do PUT de parcelamento: só o que muda nas parcelas em faturas abertas, mais a {@code versao} lida. */
public record EditarParcelamentoRequest(@NotNull String descricao, UUID metaId, List<UUID> tagIds, @NotNull Long versao) {}
