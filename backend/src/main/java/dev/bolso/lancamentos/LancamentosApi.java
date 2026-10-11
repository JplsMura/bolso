package dev.bolso.lancamentos;

import dev.bolso.compartilhado.Dinheiro;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Única porta dos outros módulos para ler lançamentos. O chamador já conferiu o acesso ao espaço; toda consulta
 * filtra por {@code espacoId} e ignora o que foi excluído.
 */
public interface LancamentosApi {

    /** Soma dos lançamentos de cada fatura. Fatura sem lançamento vem com zero. */
    Map<UUID, Dinheiro> somaPorFatura(UUID espacoId, Collection<UUID> faturaIds);

    /** Lançamentos das faturas, da compra mais recente para a mais antiga. */
    List<ItemDaFatura> itensDasFaturas(UUID espacoId, Collection<UUID> faturaIds);
}
