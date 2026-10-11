package dev.bolso.lancamentos;

import java.time.YearMonth;
import java.util.UUID;

/** O que o Lançamentos precisa saber de uma fatura: de qual cartão e mês ela é, e se ainda aceita mudanças. */
public record FaturaResolvida(UUID id, UUID cartaoId, String nomeDoCartao, YearMonth mes, boolean aberta) {}
