package dev.bolso.cartoes.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** {@code pagaEm} ausente = hoje. */
public record PagarFaturaRequest(@NotNull Long versao, LocalDate pagaEm) {}
