package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.domain.Tag;
import dev.bolso.lancamentos.domain.TipoCusto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** {@code tipoCusto} não vem quando a tag não tem tipo. */
public record TagResponse(@NotNull UUID id, @NotNull String nome, TipoCusto tipoCusto) {

    static TagResponse de(Tag tag) {
        return new TagResponse(tag.id(), tag.nome(), tag.tipoCusto());
    }
}
