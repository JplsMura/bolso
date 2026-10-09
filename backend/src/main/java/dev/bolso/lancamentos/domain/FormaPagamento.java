package dev.bolso.lancamentos.domain;

/** Como o gasto foi pago. {@code CREDIT} existe no banco mas só é aceito a partir da feature de cartões. */
public enum FormaPagamento {
    PIX,
    DEBIT,
    CREDIT,
    CASH,
    BOLETO,
    TRANSFER,
    OTHER
}
