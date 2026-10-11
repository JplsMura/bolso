package dev.bolso.cartoes.application;

import java.time.YearMonth;
import java.util.List;

public record FaturasDoMes(YearMonth mes, List<FaturaDoCartao> faturas) {}
