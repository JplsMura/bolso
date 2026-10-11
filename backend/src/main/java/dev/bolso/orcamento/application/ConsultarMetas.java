package dev.bolso.orcamento.application;

import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.orcamento.domain.Meta;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsultarMetas {

    private final MetasRepository metas;
    private final IdentidadeApi identidade;

    public ConsultarMetas(MetasRepository metas, IdentidadeApi identidade) {
        this.metas = metas;
        this.identidade = identidade;
    }

    /** Metas do espaço, na ordem de exibição. Vazio na Empresa. */
    public List<Meta> listar(UUID espacoId) {
        identidade.papelNoEspaco(espacoId); // 404 se não for membro
        return metas.listarDoEspaco(espacoId);
    }
}
