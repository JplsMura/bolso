package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.Fatura;
import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.ItemDaFatura;
import java.util.List;

/** Um cartão e a fatura dele num mês. {@code fatura} é nula se o mês não teve compra nesse cartão. */
public record FaturaDoCartao(CartaoComUso cartao, Fatura fatura, Dinheiro total, List<ItemDaFatura> itens) {}
