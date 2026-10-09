package dev.bolso.orcamento.adapter.in.web;

import dev.bolso.orcamento.application.ConsultarMetas;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/espacos/{espacoId}/metas", produces = MediaType.APPLICATION_JSON_VALUE)
class MetasController {

    private final ConsultarMetas consultarMetas;

    MetasController(ConsultarMetas consultarMetas) {
        this.consultarMetas = consultarMetas;
    }

    /** Metas do espaço, na ordem de exibição (vazio na Empresa). O nome do método vira o operationId. */
    @GetMapping
    List<MetaResponse> listarMetas(@PathVariable UUID espacoId) {
        return consultarMetas.listar(espacoId).stream().map(MetaResponse::de).toList();
    }
}
