package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.domain.DadosDoLancamento;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Corpo do POST. O formato do valor é conferido aqui (400); as regras de negócio, no domínio (422). */
public record NovoLancamentoRequest(
        @NotNull Direcao direcao,
        @NotNull @Pattern(regexp = "\\d{1,12}\\.\\d{2}", message = "use o formato 110.00") String valor,
        @NotNull String descricao,
        @NotNull LocalDate data,
        UUID metaId,
        FormaPagamento formaPagamento,
        List<UUID> tagIds) {

    DadosDoLancamento paraDados() {
        return DadosFormatados.de(direcao, valor, descricao, data, metaId, formaPagamento, tagIds);
    }

    /** Monta os dados do domínio; compartilhado entre criar e editar. */
    static final class DadosFormatados {
        private DadosFormatados() {}

        static DadosDoLancamento de(
                Direcao direcao, String valor, String descricao, LocalDate data, UUID metaId, FormaPagamento forma, List<UUID> tagIds) {
            return new DadosDoLancamento(
                    direcao,
                    Dinheiro.deTexto(valor),
                    descricao,
                    data,
                    metaId,
                    forma,
                    tagIds == null ? new HashSet<>() : new HashSet<>(tagIds));
        }
    }
}
