package dev.bolso.lancamentos.application;

import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import java.util.UUID;

/** Filtros da lista do mês; cada um é opcional (nulo = não filtra). */
public record FiltrosDeLancamento(String texto, UUID metaId, UUID tagId, FormaPagamento formaPagamento, Direcao direcao) {

    public static final FiltrosDeLancamento NENHUM = new FiltrosDeLancamento(null, null, null, null, null);
}
