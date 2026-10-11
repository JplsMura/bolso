package dev.bolso.lancamentos.application;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.ItemDaFatura;
import dev.bolso.lancamentos.LancamentosApi;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A {@link LancamentosApi} para os outros módulos (hoje, Cartões). Quem chama já conferiu o acesso ao espaço. */
@Service
@Transactional(readOnly = true)
class LancamentosApiService implements LancamentosApi {

    private final ConsultaDeLancamentos consulta;

    LancamentosApiService(ConsultaDeLancamentos consulta) {
        this.consulta = consulta;
    }

    @Override
    public Map<UUID, Dinheiro> somaPorFatura(UUID espacoId, Collection<UUID> faturaIds) {
        return faturaIds.isEmpty() ? Map.of() : consulta.somaPorFatura(espacoId, faturaIds);
    }

    @Override
    public List<ItemDaFatura> itensDasFaturas(UUID espacoId, Collection<UUID> faturaIds) {
        return faturaIds.isEmpty() ? List.of() : consulta.itensDasFaturas(espacoId, faturaIds);
    }
}
