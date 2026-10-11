package dev.bolso.lancamentos.application;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.ItemDaFatura;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída: leituras com SQL direto (lista do mês, totais, busca, parcelas, faturas). Não carrega agregados. */
public interface ConsultaDeLancamentos {

    ConsultaDoMes consultarMes(UUID espacoId, YearMonth mes, FiltrosDeLancamento filtros);

    Optional<ItemDeLancamento> buscar(UUID espacoId, UUID id);

    /** Parcelas não excluídas do parcelamento, pela ordem do número. */
    List<ItemDeLancamento> parcelasDoParcelamento(UUID espacoId, UUID parcelamentoId);

    Map<UUID, Dinheiro> somaPorFatura(UUID espacoId, Collection<UUID> faturaIds);

    List<ItemDaFatura> itensDasFaturas(UUID espacoId, Collection<UUID> faturaIds);
}
