package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.domain.DadosDoParcelamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Corpo do POST de parcelamento. {@code valorTotal} é o total da compra ("150.00"); {@code parcelaAtual} é opcional
 * (padrão 1) e serve para compra em andamento. Os limites (2 a 48 parcelas etc.) são regra de negócio (422).
 */
public record NovoParcelamentoRequest(
        @NotNull @Pattern(regexp = "\\d{1,12}\\.\\d{2}", message = "use o formato 110.00") String valorTotal,
        @NotNull Integer parcelas,
        Integer parcelaAtual,
        @NotNull String descricao,
        @NotNull LocalDate data,
        @NotNull UUID cartaoId,
        UUID metaId,
        List<UUID> tagIds) {

    DadosDoParcelamento paraDados() {
        return new DadosDoParcelamento(
                Dinheiro.deTexto(valorTotal),
                parcelas,
                parcelaAtual == null ? 1 : parcelaAtual,
                descricao,
                data,
                cartaoId,
                metaId,
                tagIds == null ? new HashSet<>() : new HashSet<>(tagIds));
    }
}
