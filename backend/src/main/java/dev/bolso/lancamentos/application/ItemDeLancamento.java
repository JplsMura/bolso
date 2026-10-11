package dev.bolso.lancamentos.application;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Um lançamento como a tela o vê, com os nomes das tags. */
public record ItemDeLancamento(
        UUID id,
        Direcao direcao,
        Dinheiro valor,
        String descricao,
        LocalDate data,
        YearMonth mesReferencia,
        UUID metaId,
        FormaPagamento formaPagamento,
        List<TagResumo> tags,
        long versao) {}
