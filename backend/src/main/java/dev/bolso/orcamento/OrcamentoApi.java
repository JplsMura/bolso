package dev.bolso.orcamento;

import java.util.UUID;

/** Única porta dos outros módulos para o Orçamento. */
public interface OrcamentoApi {

    /** A meta existe e pertence a este espaço. */
    boolean metaExisteNoEspaco(UUID espacoId, UUID metaId);
}
