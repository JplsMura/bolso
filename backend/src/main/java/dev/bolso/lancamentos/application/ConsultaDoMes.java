package dev.bolso.lancamentos.application;

import dev.bolso.lancamentos.domain.TotaisDoMes;
import java.time.YearMonth;
import java.util.List;

/** Itens do mês (já filtrados) e os totais do mês inteiro. */
public record ConsultaDoMes(YearMonth mes, TotaisDoMes totais, List<ItemDeLancamento> itens) {}
