package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.StatusDaFatura;
import java.time.LocalDate;
import java.time.YearMonth;

/** Em qual fatura cai uma compra numa data. {@code status} é nulo se a fatura ainda não existe. */
public record PrevisaoDaFatura(YearMonth mes, LocalDate fechamento, LocalDate vencimento, StatusDaFatura status) {}
