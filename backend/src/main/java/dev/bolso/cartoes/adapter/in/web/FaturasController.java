package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.application.ConsultarFaturas;
import dev.bolso.cartoes.application.MudarStatusDaFatura;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Faturas do mês e as mudanças de status. Pagar não cria lançamento. */
@RestController
@RequestMapping(path = "/api/v1/espacos/{espacoId}/faturas", produces = MediaType.APPLICATION_JSON_VALUE)
class FaturasController {

    private final ConsultarFaturas consultar;
    private final MudarStatusDaFatura mudar;

    FaturasController(ConsultarFaturas consultar, MudarStatusDaFatura mudar) {
        this.consultar = consultar;
        this.mudar = mudar;
    }

    /** Um item por cartão ativo. {@code mes} ausente = mês atual. */
    @GetMapping
    FaturasDoMesResponse listarFaturas(
            @PathVariable UUID espacoId,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "use o formato 2026-03")
                    String mes) {
        return FaturasDoMesResponse.de(consultar.doMes(espacoId, mes == null ? null : YearMonth.parse(mes)));
    }

    @PostMapping(path = "/{id}/fechar", consumes = MediaType.APPLICATION_JSON_VALUE)
    FaturaDoCartaoResponse fecharFatura(
            @PathVariable UUID espacoId, @PathVariable UUID id, @Valid @RequestBody MudarStatusRequest corpo) {
        return FaturaDoCartaoResponse.de(mudar.fechar(espacoId, id, corpo.versao()));
    }

    @PostMapping(path = "/{id}/pagar", consumes = MediaType.APPLICATION_JSON_VALUE)
    FaturaDoCartaoResponse pagarFatura(
            @PathVariable UUID espacoId, @PathVariable UUID id, @Valid @RequestBody PagarFaturaRequest corpo) {
        return FaturaDoCartaoResponse.de(mudar.pagar(espacoId, id, corpo.pagaEm(), corpo.versao()));
    }

    @PostMapping(path = "/{id}/reabrir", consumes = MediaType.APPLICATION_JSON_VALUE)
    FaturaDoCartaoResponse reabrirFatura(
            @PathVariable UUID espacoId, @PathVariable UUID id, @Valid @RequestBody MudarStatusRequest corpo) {
        return FaturaDoCartaoResponse.de(mudar.reabrir(espacoId, id, corpo.versao()));
    }

    @PostMapping(path = "/{id}/desfazer-pagamento", consumes = MediaType.APPLICATION_JSON_VALUE)
    FaturaDoCartaoResponse desfazerPagamentoDaFatura(
            @PathVariable UUID espacoId, @PathVariable UUID id, @Valid @RequestBody MudarStatusRequest corpo) {
        return FaturaDoCartaoResponse.de(mudar.desfazerPagamento(espacoId, id, corpo.versao()));
    }
}
