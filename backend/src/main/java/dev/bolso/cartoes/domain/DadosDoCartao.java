package dev.bolso.cartoes.domain;

import dev.bolso.compartilhado.Dinheiro;

/** O que o usuário informa ao cadastrar ou editar um cartão. */
public record DadosDoCartao(String nome, int diaFechamento, int diaVencimento, Dinheiro limite) {}
