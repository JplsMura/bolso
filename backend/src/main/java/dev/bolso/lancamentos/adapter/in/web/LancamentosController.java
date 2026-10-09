package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.application.ConsultarLancamentos;
import dev.bolso.lancamentos.application.FiltrosDeLancamento;
import dev.bolso.lancamentos.application.GravarLancamentos;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.YearMonth;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Os nomes dos métodos viram os operationId do OpenAPI. */
@RestController
@RequestMapping(path = "/api/v1/espacos/{espacoId}/lancamentos", produces = MediaType.APPLICATION_JSON_VALUE)
class LancamentosController {

    private final ConsultarLancamentos consultar;
    private final GravarLancamentos gravar;

    LancamentosController(ConsultarLancamentos consultar, GravarLancamentos gravar) {
        this.consultar = consultar;
        this.gravar = gravar;
    }

    /** Lançamentos do mês e os totais. {@code mes} ausente = mês atual. */
    @GetMapping
    ListaDoMesResponse listarLancamentos(
            @PathVariable UUID espacoId,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "use o formato 2026-03") String mes,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID metaId,
            @RequestParam(required = false) UUID tagId,
            @RequestParam(required = false) FormaPagamento pagamento,
            @RequestParam(required = false) Direcao direcao) {
        var filtros = new FiltrosDeLancamento(q, metaId, tagId, pagamento, direcao);
        return ListaDoMesResponse.de(consultar.consultarMes(espacoId, mes == null ? null : YearMonth.parse(mes), filtros));
    }

    @GetMapping("/{id}")
    LancamentoResponse buscarLancamento(@PathVariable UUID espacoId, @PathVariable UUID id) {
        return LancamentoResponse.de(consultar.buscar(espacoId, id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<LancamentoResponse> criarLancamento(
            @PathVariable UUID espacoId, @Valid @RequestBody NovoLancamentoRequest corpo) {
        var item = gravar.lancar(espacoId, corpo.paraDados());
        var local = URI.create("/api/v1/espacos/" + espacoId + "/lancamentos/" + item.id());
        return ResponseEntity.created(local).body(LancamentoResponse.de(item));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    LancamentoResponse editarLancamento(
            @PathVariable UUID espacoId, @PathVariable UUID id, @Valid @RequestBody EditarLancamentoRequest corpo) {
        return LancamentoResponse.de(gravar.editar(espacoId, id, corpo.paraDados(), corpo.versao()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void excluirLancamento(@PathVariable UUID espacoId, @PathVariable UUID id) {
        gravar.excluir(espacoId, id);
    }
}
