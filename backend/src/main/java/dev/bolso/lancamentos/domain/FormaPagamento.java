package dev.bolso.lancamentos.domain;

/** Como o gasto foi pago. {@code CREDIT} exige um cartão do espaço. */
public enum FormaPagamento {
    PIX,
    DEBIT,
    CREDIT,
    CASH,
    BOLETO,
    TRANSFER,
    OTHER
}
