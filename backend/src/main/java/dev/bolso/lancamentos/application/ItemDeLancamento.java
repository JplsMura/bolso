package dev.bolso.lancamentos.application;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import dev.bolso.lancamentos.domain.Parcela;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Um lançamento como a tela o vê, com os nomes das tags. {@code cartaoId}, {@code faturaId} e {@code faturaMes}
 * só existem no crédito; {@code parcela}, só em parcelamento. {@code faturaMes} vem do módulo Cartões e é
 * preenchido pelo caso de uso (a consulta devolve nulo).
 */
public record ItemDeLancamento(
        UUID id,
        Direcao direcao,
        Dinheiro valor,
        String descricao,
        LocalDate data,
        YearMonth mesReferencia,
        UUID metaId,
        FormaPagamento formaPagamento,
        UUID cartaoId,
        UUID faturaId,
        YearMonth faturaMes,
        Parcela parcela,
        List<TagResumo> tags,
        long versao) {

    public ItemDeLancamento comFaturaMes(YearMonth mes) {
        return new ItemDeLancamento(
                id, direcao, valor, descricao, data, mesReferencia, metaId, formaPagamento, cartaoId, faturaId, mes, parcela,
                tags, versao);
    }

    public ItemDeLancamento comTags(List<TagResumo> novasTags) {
        return new ItemDeLancamento(
                id, direcao, valor, descricao, data, mesReferencia, metaId, formaPagamento, cartaoId, faturaId, faturaMes,
                parcela, List.copyOf(novasTags), versao);
    }
}
