package dev.bolso.lancamentos.domain;

import dev.bolso.compartilhado.Dinheiro;
import java.time.LocalDate;

/**
 * Uma parcela a criar. {@code data} é a da compra mais {@code numero - 1} meses (dia 31 vira o último dia nos meses
 * curtos): é nela que a parcela conta no orçamento. {@code mesesDepoisDaPrimeira} diz em qual fatura ela cai,
 * contando da fatura da compra.
 */
public record ParcelaPlanejada(int numero, Dinheiro valor, LocalDate data, int mesesDepoisDaPrimeira) {}
