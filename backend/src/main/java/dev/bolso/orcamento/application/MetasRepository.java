package dev.bolso.orcamento.application;

import dev.bolso.orcamento.domain.Meta;
import java.util.List;
import java.util.UUID;

/** Porta de saída: metas de um espaço. */
public interface MetasRepository {

    List<Meta> listarDoEspaco(UUID espacoId);

    boolean existeNoEspaco(UUID espacoId, UUID metaId);
}
