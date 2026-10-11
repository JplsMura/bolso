package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.domain.DadosDoLancamento;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Corpo do PUT: os mesmos campos do POST mais a {@code versao} que a tela leu. */
public record EditarLancamentoRequest(
        @NotNull Direcao direcao,
        @NotNull @Pattern(regexp = "\\d{1,12}\\.\\d{2}", message = "use o formato 110.00") String valor,
        @NotNull String descricao,
        @NotNull LocalDate data,
        UUID metaId,
        FormaPagamento formaPagamento,
        List<UUID> tagIds,
        @NotNull Long versao) {

    DadosDoLancamento paraDados() {
        return NovoLancamentoRequest.DadosFormatados.de(direcao, valor, descricao, data, metaId, formaPagamento, tagIds);
    }
}
