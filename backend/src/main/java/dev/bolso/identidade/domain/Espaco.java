package dev.bolso.identidade.domain;

import dev.bolso.identidade.Papel;
import java.util.Objects;
import java.util.UUID;

/** Um espaço visto por um usuário: o papel é o dele nesse espaço. */
public record Espaco(UUID id, TipoEspaco tipo, String nome, Papel papel) {

    public Espaco {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(papel, "papel");
    }
}
