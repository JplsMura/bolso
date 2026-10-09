package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.application.GerenciarTags;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/espacos/{espacoId}/tags", produces = MediaType.APPLICATION_JSON_VALUE)
class TagsController {

    private final GerenciarTags gerenciarTags;

    TagsController(GerenciarTags gerenciarTags) {
        this.gerenciarTags = gerenciarTags;
    }

    @GetMapping
    List<TagResponse> listarTags(@PathVariable UUID espacoId) {
        return gerenciarTags.listar(espacoId).stream().map(TagResponse::de).toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TagResponse> criarTag(@PathVariable UUID espacoId, @Valid @RequestBody NovaTagRequest corpo) {
        var tag = gerenciarTags.criar(espacoId, corpo.nome(), corpo.tipoCusto());
        return ResponseEntity.created(URI.create("/api/v1/espacos/" + espacoId + "/tags/" + tag.id()))
                .body(TagResponse.de(tag));
    }
}
