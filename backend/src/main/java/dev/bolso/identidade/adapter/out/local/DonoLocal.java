package dev.bolso.identidade.adapter.out.local;

import dev.bolso.identidade.application.UsuarioAtual;
import dev.bolso.identidade.domain.UsuarioId;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Enquanto não há login (feature 007), todo chamador é o dono local, criado pela migração V3.
 * A 007 troca este adaptador por um que lê a sessão.
 */
@Component
class DonoLocal implements UsuarioAtual {

    /** Mesmo id inserido em V3__dono_local_e_espacos_iniciais.sql. */
    static final UUID ID = UUID.fromString("019a0000-0000-7000-8000-000000000001");

    @Override
    public UsuarioId id() {
        return new UsuarioId(ID);
    }
}
