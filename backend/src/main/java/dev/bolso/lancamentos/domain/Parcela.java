package dev.bolso.lancamentos.domain;

import java.util.UUID;

/** Este lançamento é a parcela {@code numero} de {@code total} do parcelamento {@code parcelamentoId}. */
public record Parcela(UUID parcelamentoId, int numero, int total) {}
