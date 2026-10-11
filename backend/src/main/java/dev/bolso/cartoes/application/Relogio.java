package dev.bolso.cartoes.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/** Porta de saída: a hora. "Hoje" e o mês atual são os de America/Sao_Paulo. */
public interface Relogio {

    Instant agora();

    LocalDate hoje();

    default YearMonth mesAtual() {
        return YearMonth.from(hoje());
    }
}
