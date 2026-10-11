package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.application.ItemDeLancamento;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contrato da API. {@code valor} é texto ("110.00"); {@code mesReferencia} e {@code faturaMes} são "2026-10".
 * {@code metaId} e {@code formaPagamento} não vêm quando não se aplicam (entrada); {@code cartaoId} e
 * {@code faturaMes}, só no crédito; {@code parcela}, só em parcelamento. {@code @NotNull} só marca o obrigatório no
 * OpenAPI.
 */
public record LancamentoResponse(
        @NotNull UUID id,
        @NotNull Direcao direcao,
        @NotNull String valor,
        @NotNull String descricao,
        @NotNull LocalDate data,
        @NotNull String mesReferencia,
        UUID metaId,
        FormaPagamento formaPagamento,
        UUID cartaoId,
        String faturaMes,
        ParcelaResponse parcela,
        @NotNull List<TagResumoResponse> tags,
        @NotNull Long versao) {

    static LancamentoResponse de(ItemDeLancamento i) {
        return new LancamentoResponse(
                i.id(),
                i.direcao(),
                i.valor().toString(),
                i.descricao(),
                i.data(),
                i.mesReferencia().toString(),
                i.metaId(),
                i.formaPagamento(),
                i.cartaoId(),
                i.faturaMes() == null ? null : i.faturaMes().toString(),
                ParcelaResponse.de(i.parcela()),
                i.tags().stream().map(TagResumoResponse::de).toList(),
                i.versao());
    }
}
