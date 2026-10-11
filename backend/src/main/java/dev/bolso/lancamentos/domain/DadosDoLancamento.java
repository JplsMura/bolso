package dev.bolso.lancamentos.domain;

import dev.bolso.compartilhado.Dinheiro;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** O que o usuário informa ao lançar ou editar. As regras ficam em {@link Lancamento}. */
public record DadosDoLancamento(
        Direcao direcao,
        Dinheiro valor,
        String descricao,
        LocalDate data,
        UUID metaId,
        FormaPagamento formaPagamento,
        Set<UUID> tagIds) {}
