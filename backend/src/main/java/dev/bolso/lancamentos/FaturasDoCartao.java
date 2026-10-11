package dev.bolso.lancamentos;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta que o Lançamentos usa para saber em qual fatura cai um gasto no cartão. Quem implementa é o módulo
 * Cartões: a dependência fica num sentido só ({@code cartoes → lancamentos}), sem ciclo entre módulos.
 * O chamador já conferiu o acesso ao espaço; toda busca filtra por {@code espacoId}.
 */
public interface FaturasDoCartao {

    /**
     * Mês da fatura em que cai uma compra feita nesta data, pelo dia de fechamento do cartão. Não grava nada.
     *
     * @return vazio se o cartão não existe neste espaço ou está arquivado
     */
    Optional<YearMonth> mesDaFaturaDaCompra(UUID espacoId, UUID cartaoId, LocalDate data);

    /**
     * A fatura do cartão naquele mês, criada (aberta) se ainda não existe. Trava a fatura para leitura até o fim
     * da transação, para ela não ser fechada enquanto a compra entra. Só para cartões já conferidos por
     * {@link #mesDaFaturaDaCompra}.
     */
    FaturaResolvida faturaDoMes(UUID espacoId, UUID cartaoId, YearMonth mes);

    /**
     * A fatura deste espaço, travada para leitura como em {@link #faturaDoMes}: usada antes de mudar ou tirar um
     * gasto dela.
     */
    Optional<FaturaResolvida> faturaTravada(UUID espacoId, UUID faturaId);

    /** As faturas pedidas que existem neste espaço, por id. Só leitura, sem trava. */
    Map<UUID, FaturaResolvida> faturas(UUID espacoId, Collection<UUID> faturaIds);
}
