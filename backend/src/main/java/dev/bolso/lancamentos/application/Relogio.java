package dev.bolso.lancamentos.application;

import java.time.Instant;
import java.time.YearMonth;

/** Porta de saída: a hora. O mês atual é o de America/Sao_Paulo. */
public interface Relogio {

    Instant agora();

    YearMonth mesAtual();
}
