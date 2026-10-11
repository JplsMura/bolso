package dev.bolso.identidade;

import java.util.UUID;

/** O usuário é membro do espaço, mas o papel dele não permite gravar. A API responde 403. */
public class SemPermissao extends RuntimeException {

    public SemPermissao(UUID espacoId, Papel papel) {
        super("O papel " + papel + " não pode alterar o espaço " + espacoId);
    }
}
