package dev.bolso.lancamentos.domain;

import java.util.UUID;

/** Alguém alterou o lançamento (ou o parcelamento) depois da leitura. A API responde 409. */
public class ConflitoDeVersao extends RuntimeException {

    public ConflitoDeVersao(UUID id) {
        super("O registro " + id + " foi alterado por outra operação");
    }
}
