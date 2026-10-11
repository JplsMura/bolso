package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.application.ConsultarParcelamentos;
import dev.bolso.lancamentos.application.GravarParcelamentos;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Compra parcelada no cartão. Os nomes dos métodos viram os operationId do OpenAPI. */
@RestController
@RequestMapping(path = "/api/v1/espacos/{espacoId}/parcelamentos", produces = MediaType.APPLICATION_JSON_VALUE)
class ParcelamentosController {

    private final ConsultarParcelamentos consultar;
    private final GravarParcelamentos gravar;

    ParcelamentosController(ConsultarParcelamentos consultar, GravarParcelamentos gravar) {
        this.consultar = consultar;
        this.gravar = gravar;
    }

    @GetMapping("/{id}")
    ParcelamentoResponse buscarParcelamento(@PathVariable UUID espacoId, @PathVariable UUID id) {
        return ParcelamentoResponse.de(consultar.buscar(espacoId, id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<ParcelamentoResponse> criarParcelamento(
            @PathVariable UUID espacoId, @Valid @RequestBody NovoParcelamentoRequest corpo) {
        var visao = gravar.criar(espacoId, corpo.paraDados());
        var local = URI.create("/api/v1/espacos/" + espacoId + "/parcelamentos/" + visao.parcelamento().id());
        return ResponseEntity.created(local).body(ParcelamentoResponse.de(visao));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ParcelamentoResponse editarParcelamento(
            @PathVariable UUID espacoId, @PathVariable UUID id, @Valid @RequestBody EditarParcelamentoRequest corpo) {
        var tags = corpo.tagIds() == null ? new HashSet<UUID>() : new HashSet<>(corpo.tagIds());
        return ParcelamentoResponse.de(gravar.editar(espacoId, id, corpo.descricao(), corpo.metaId(), tags, corpo.versao()));
    }

    /** Exclui as parcelas em faturas abertas; as de faturas fechadas ou pagas ficam. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void excluirParcelamento(@PathVariable UUID espacoId, @PathVariable UUID id) {
        gravar.excluir(espacoId, id);
    }
}
