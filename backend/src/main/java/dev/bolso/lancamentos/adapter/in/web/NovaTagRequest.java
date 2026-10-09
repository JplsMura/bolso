package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.domain.TipoCusto;
import jakarta.validation.constraints.NotNull;

public record NovaTagRequest(@NotNull String nome, TipoCusto tipoCusto) {}
