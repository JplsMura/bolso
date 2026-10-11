package dev.bolso.lancamentos.domain;

import dev.bolso.compartilhado.Dinheiro;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * O que o usuário informa ao parcelar uma compra no cartão. {@code parcelaAtual} é 1 numa compra nova; numa compra
 * em andamento (de antes do histórico) é a parcela que cai agora, e só ela e as seguintes são criadas.
 */
public record DadosDoParcelamento(
        Dinheiro valorTotal,
        int parcelas,
        int parcelaAtual,
        String descricao,
        LocalDate dataDaCompra,
        UUID cartaoId,
        UUID metaId,
        Set<UUID> tagIds) {}
