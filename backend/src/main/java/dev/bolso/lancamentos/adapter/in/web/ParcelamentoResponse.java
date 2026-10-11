package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.application.ParcelaDoParcelamento;
import dev.bolso.lancamentos.application.VisaoDoParcelamento;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O parcelamento e as parcelas que sobram. {@code metaId} e {@code tags} são os da última parcela (as edições valem
 * para as faturas abertas, que são as mais novas). {@code faturaAberta} diz se a parcela ainda pode mudar.
 */
public record ParcelamentoResponse(
        @NotNull UUID id,
        @NotNull String descricao,
        @NotNull String valorTotal,
        @NotNull Integer parcelas,
        @NotNull Integer parcelaAtual,
        @NotNull LocalDate data,
        @NotNull UUID cartaoId,
        UUID metaId,
        @NotNull List<TagResumoResponse> tags,
        @NotNull Long versao,
        @NotNull List<ParcelaDoParcelamentoResponse> itens) {

    public record ParcelaDoParcelamentoResponse(
            @NotNull UUID lancamentoId,
            @NotNull Integer numero,
            @NotNull String valor,
            @NotNull LocalDate data,
            String faturaMes,
            @NotNull Boolean faturaAberta) {

        static ParcelaDoParcelamentoResponse de(ParcelaDoParcelamento p) {
            var i = p.item();
            return new ParcelaDoParcelamentoResponse(
                    i.id(),
                    i.parcela().numero(),
                    i.valor().toString(),
                    i.data(),
                    i.faturaMes() == null ? null : i.faturaMes().toString(),
                    p.faturaAberta());
        }
    }

    static ParcelamentoResponse de(VisaoDoParcelamento v) {
        var p = v.parcelamento();
        return new ParcelamentoResponse(
                p.id(),
                p.descricao(),
                p.valorTotal().toString(),
                p.parcelas(),
                p.parcelaAtual(),
                p.dataDaCompra(),
                p.cartaoId(),
                v.metaId(),
                v.tags().stream().map(TagResumoResponse::de).toList(),
                p.versao(),
                v.parcelas().stream().map(ParcelaDoParcelamentoResponse::de).toList());
    }
}
