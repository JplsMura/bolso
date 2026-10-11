package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.application.ConsultarFaturas;
import dev.bolso.cartoes.application.GerenciarCartoes;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Cadastro de cartões. Os nomes dos métodos viram os operationId do OpenAPI. */
@RestController
@RequestMapping(path = "/api/v1/espacos/{espacoId}/cartoes", produces = MediaType.APPLICATION_JSON_VALUE)
class CartoesController {

    private final GerenciarCartoes gerenciar;
    private final ConsultarFaturas faturas;

    CartoesController(GerenciarCartoes gerenciar, ConsultarFaturas faturas) {
        this.gerenciar = gerenciar;
        this.faturas = faturas;
    }

    @GetMapping
    List<CartaoResponse> listarCartoes(@PathVariable UUID espacoId) {
        return gerenciar.listar(espacoId).stream().map(CartaoResponse::de).toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<CartaoResponse> criarCartao(@PathVariable UUID espacoId, @Valid @RequestBody NovoCartaoRequest corpo) {
        var cartao = gerenciar.criar(espacoId, corpo.paraDados());
        var local = URI.create("/api/v1/espacos/" + espacoId + "/cartoes/" + cartao.cartao().id());
        return ResponseEntity.created(local).body(CartaoResponse.de(cartao));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    CartaoResponse editarCartao(
            @PathVariable UUID espacoId, @PathVariable UUID id, @Valid @RequestBody EditarCartaoRequest corpo) {
        return CartaoResponse.de(gerenciar.editar(espacoId, id, corpo.paraDados(), corpo.versao()));
    }

    /** Arquiva (não apaga). 422 se houver fatura não paga. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void arquivarCartao(@PathVariable UUID espacoId, @PathVariable UUID id) {
        gerenciar.arquivar(espacoId, id);
    }

    /** Em qual fatura cai uma compra nesta data (não cria nada). */
    @GetMapping("/{id}/fatura-da-compra")
    FaturaDaCompraResponse faturaDaCompra(
            @PathVariable UUID espacoId,
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return FaturaDaCompraResponse.de(faturas.faturaDaCompra(espacoId, id, data));
    }
}
