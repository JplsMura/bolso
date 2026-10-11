package dev.bolso.cartoes.domain;

import java.util.UUID;

/** Alguém alterou o cartão ou a fatura depois da leitura. A API responde 409. */
public class VersaoDesatualizada extends RuntimeException {

    public VersaoDesatualizada(UUID id) {
        super("O registro " + id + " foi alterado por outra operação");
    }
}
