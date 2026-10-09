package dev.bolso.orcamento.application;

import dev.bolso.orcamento.OrcamentoApi;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class OrcamentoApiService implements OrcamentoApi {

    private final MetasRepository metas;

    OrcamentoApiService(MetasRepository metas) {
        this.metas = metas;
    }

    @Override
    public boolean metaExisteNoEspaco(UUID espacoId, UUID metaId) {
        return metas.existeNoEspaco(espacoId, metaId);
    }
}
