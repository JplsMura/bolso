package dev.bolso.cartoes.domain;

/** Aberta (recebe compras), fechada (total congelado) ou paga. */
public enum StatusDaFatura {
    OPEN,
    CLOSED,
    PAID
}
