package dev.bolso.orcamento.domain;

import java.util.UUID;

/** Uma das 6 metas do espaço Casa (Custos Fixos, Conforto...). A ordem e a cor vêm do cadastro. */
public record Meta(UUID id, String nome, String cor, int ordem) {}
