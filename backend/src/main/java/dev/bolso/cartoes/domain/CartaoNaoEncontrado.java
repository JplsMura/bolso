package dev.bolso.cartoes.domain;

import java.util.UUID;

/** O cartão não existe neste espaço, ou foi arquivado. A API responde 404. */
public class CartaoNaoEncontrado extends RuntimeException {

    public CartaoNaoEncontrado(UUID id) {
        super("Cartão não encontrado: " + id);
    }
}
