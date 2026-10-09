package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.application.TagResumo;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TagResumoResponse(@NotNull UUID id, @NotNull String nome) {

    static TagResumoResponse de(TagResumo tag) {
        return new TagResumoResponse(tag.id(), tag.nome());
    }
}
