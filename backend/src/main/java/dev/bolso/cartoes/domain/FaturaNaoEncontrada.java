package dev.bolso.cartoes.domain;

import java.util.UUID;

/** A fatura não existe neste espaço. A API responde 404. */
public class FaturaNaoEncontrada extends RuntimeException {

    public FaturaNaoEncontrada(UUID id) {
        super("Fatura não encontrada: " + id);
    }
}
