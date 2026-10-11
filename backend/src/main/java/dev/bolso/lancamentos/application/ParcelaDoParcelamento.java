package dev.bolso.lancamentos.application;

/** Uma parcela como a tela do parcelamento a vê: o lançamento e se a fatura dela ainda está aberta. */
public record ParcelaDoParcelamento(ItemDeLancamento item, boolean faturaAberta) {}
