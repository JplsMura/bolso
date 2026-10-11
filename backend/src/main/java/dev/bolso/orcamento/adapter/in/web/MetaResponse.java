package dev.bolso.orcamento.adapter.in.web;

import dev.bolso.orcamento.domain.Meta;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** {@code @NotNull} só existe para o OpenAPI marcar os campos como obrigatórios. */
public record MetaResponse(@NotNull UUID id, @NotNull String nome, @NotNull String cor, @NotNull Integer ordem) {

    static MetaResponse de(Meta meta) {
        return new MetaResponse(meta.id(), meta.nome(), meta.cor(), meta.ordem());
    }
}
