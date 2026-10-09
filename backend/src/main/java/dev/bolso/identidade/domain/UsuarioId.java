package dev.bolso.identidade.domain;

import java.util.Objects;
import java.util.UUID;

public record UsuarioId(UUID valor) {

    public UsuarioId {
        Objects.requireNonNull(valor, "valor");
    }
}
