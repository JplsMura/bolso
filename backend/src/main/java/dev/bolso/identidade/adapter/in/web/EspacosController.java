package dev.bolso.identidade.adapter.in.web;

import dev.bolso.identidade.application.ConsultarEspacos;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/espacos", produces = MediaType.APPLICATION_JSON_VALUE)
class EspacosController {

    private final ConsultarEspacos consultarEspacos;

    EspacosController(ConsultarEspacos consultarEspacos) {
        this.consultarEspacos = consultarEspacos;
    }

    /** Espaços do usuário atual, Casa primeiro. O nome do método vira o operationId do OpenAPI. */
    @GetMapping
    List<EspacoResponse> listarEspacos() {
        return consultarEspacos.listar().stream().map(EspacoResponse::de).toList();
    }

    @GetMapping("/{id}")
    EspacoResponse buscarEspaco(@PathVariable UUID id) {
        return EspacoResponse.de(consultarEspacos.buscar(id));
    }
}
