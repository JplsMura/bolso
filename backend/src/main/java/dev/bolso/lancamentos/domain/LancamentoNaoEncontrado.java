package dev.bolso.lancamentos.domain;

import java.util.UUID;

/** O lançamento não existe neste espaço, ou foi excluído. A API responde 404. */
public class LancamentoNaoEncontrado extends RuntimeException {

    public LancamentoNaoEncontrado(UUID id) {
        super("Lançamento não encontrado: " + id);
    }
}
