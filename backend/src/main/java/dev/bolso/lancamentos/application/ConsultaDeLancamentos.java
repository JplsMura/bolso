package dev.bolso.lancamentos.application;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída: leituras da tela (lista do mês, totais, busca). Não carrega agregados. */
public interface ConsultaDeLancamentos {

    ConsultaDoMes consultarMes(UUID espacoId, YearMonth mes, FiltrosDeLancamento filtros);

    Optional<ItemDeLancamento> buscar(UUID espacoId, UUID id);
}
