package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.Fatura;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída: faturas do espaço. */
public interface FaturasRepository {

    Optional<Fatura> buscar(UUID espacoId, UUID id);

    /**
     * A fatura travada para escrita até o fim da transação: um gasto não entra nela enquanto ela muda de status
     * (o Lançamentos trava para leitura antes de gravar).
     */
    Optional<Fatura> buscarParaMudar(UUID espacoId, UUID id);

    /** Grava a mudança de status. Lança {@code VersaoDesatualizada} se outra operação alterou antes. */
    void salvar(Fatura fatura, Instant agora);

    /** A fatura do cartão no mês; se não existe, grava a informada. Devolve a que ficou, travada para leitura. */
    Fatura obterOuCriar(Fatura nova);

    Optional<Fatura> doCartaoNoMes(UUID espacoId, UUID cartaoId, YearMonth mes);

    /** As faturas pedidas deste espaço; {@code travar} trava para leitura até o fim da transação. */
    List<Fatura> porIds(UUID espacoId, Collection<UUID> ids, boolean travar);

    /** As faturas do mês, de todos os cartões do espaço. */
    List<Fatura> doMes(UUID espacoId, YearMonth mes);

    /** Faturas abertas ou fechadas (as que ocupam limite) de todos os cartões do espaço. */
    List<Fatura> naoPagas(UUID espacoId);
}
