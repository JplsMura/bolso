package dev.bolso.cartoes.adapter.in.web;

import jakarta.validation.constraints.NotNull;

/** Fechar, reabrir ou desfazer pagamento: só a versão lida. */
public record MudarStatusRequest(@NotNull Long versao) {}
