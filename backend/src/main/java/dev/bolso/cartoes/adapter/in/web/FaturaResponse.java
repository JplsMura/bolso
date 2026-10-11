package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.application.FaturaDoCartao;
import dev.bolso.cartoes.domain.StatusDaFatura;
import dev.bolso.lancamentos.ItemDaFatura;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Uma fatura com o total (congelado, se fechada) e os gastos. {@code pagaEm} só na paga. */
public record FaturaResponse(
        @NotNull UUID id,
        @NotNull String mes,
        @NotNull StatusDaFatura status,
        @NotNull LocalDate fechamento,
        @NotNull LocalDate vencimento,
        @NotNull String total,
        LocalDate pagaEm,
        @NotNull Long versao,
        @NotNull List<ItemDaFaturaResponse> itens) {

    /** Um gasto da fatura. {@code metaId} não vem na Empresa; {@code parcela}, fora de parcelamento. */
    public record ItemDaFaturaResponse(
            @NotNull UUID lancamentoId,
            @NotNull String descricao,
            @NotNull LocalDate data,
            UUID metaId,
            @NotNull String valor,
            ParcelaDoItemDaFaturaResponse parcela) {

        static ItemDaFaturaResponse de(ItemDaFatura i) {
            return new ItemDaFaturaResponse(
                    i.lancamentoId(),
                    i.descricao(),
                    i.data(),
                    i.metaId(),
                    i.valor().toString(),
                    i.numeroDaParcela() == null ? null : new ParcelaDoItemDaFaturaResponse(i.numeroDaParcela(), i.totalDeParcelas()));
        }
    }

    /** "2/6". */
    public record ParcelaDoItemDaFaturaResponse(@NotNull Integer numero, @NotNull Integer total) {}

    static FaturaResponse de(FaturaDoCartao f) {
        var fatura = f.fatura();
        if (fatura == null) {
            return null;
        }
        return new FaturaResponse(
                fatura.id(),
                fatura.mes().toString(),
                fatura.status(),
                fatura.fechamento(),
                fatura.vencimento(),
                f.total().toString(),
                fatura.pagaEm(),
                fatura.versao(),
                f.itens().stream().map(ItemDaFaturaResponse::de).toList());
    }
}
