package dev.bolso.lancamentos.domain;

import java.util.UUID;

/** O parcelamento não existe neste espaço, ou foi excluído. A API responde 404. */
public class ParcelamentoNaoEncontrado extends RuntimeException {

    public ParcelamentoNaoEncontrado(UUID id) {
        super("Parcelamento não encontrado: " + id);
    }
}
