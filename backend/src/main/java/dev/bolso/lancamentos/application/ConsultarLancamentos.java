package dev.bolso.lancamentos.application;

import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.lancamentos.FaturaResolvida;
import dev.bolso.lancamentos.FaturasDoCartao;
import dev.bolso.lancamentos.domain.LancamentoNaoEncontrado;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leituras: lista do mês com filtros e busca, e um lançamento. O mês da fatura vem do módulo Cartões. */
@Service
@Transactional(readOnly = true)
public class ConsultarLancamentos {

    private final ConsultaDeLancamentos consulta;
    private final FaturasDoCartao faturas;
    private final IdentidadeApi identidade;
    private final Relogio relogio;

    public ConsultarLancamentos(
            ConsultaDeLancamentos consulta, FaturasDoCartao faturas, IdentidadeApi identidade, Relogio relogio) {
        this.consulta = consulta;
        this.faturas = faturas;
        this.identidade = identidade;
        this.relogio = relogio;
    }

    /** {@code mes} nulo = mês atual. Texto em branco não filtra. */
    public ConsultaDoMes consultarMes(UUID espacoId, YearMonth mes, FiltrosDeLancamento filtros) {
        identidade.papelNoEspaco(espacoId); // 404 se não for membro
        var texto = filtros.texto() == null || filtros.texto().isBlank() ? null : filtros.texto().trim();
        var efetivos = new FiltrosDeLancamento(
                texto, filtros.metaId(), filtros.tagId(), filtros.formaPagamento(), filtros.direcao(), filtros.cartaoId());
        var resultado = consulta.consultarMes(espacoId, mes != null ? mes : relogio.mesAtual(), efetivos);
        return new ConsultaDoMes(resultado.mes(), resultado.totais(), comMesDaFatura(espacoId, resultado.itens()));
    }

    public ItemDeLancamento buscar(UUID espacoId, UUID id) {
        identidade.papelNoEspaco(espacoId);
        var item = consulta.buscar(espacoId, id).orElseThrow(() -> new LancamentoNaoEncontrado(id));
        return comMesDaFatura(espacoId, List.of(item)).getFirst();
    }

    /** Preenche o mês da fatura dos gastos no cartão (uma consulta só ao módulo Cartões). */
    public List<ItemDeLancamento> comMesDaFatura(UUID espacoId, List<ItemDeLancamento> itens) {
        var ids = itens.stream().map(ItemDeLancamento::faturaId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return itens;
        }
        Map<UUID, FaturaResolvida> porId = faturas.faturas(espacoId, ids);
        return itens.stream()
                .map(i -> i.faturaId() == null || !porId.containsKey(i.faturaId())
                        ? i
                        : i.comFaturaMes(porId.get(i.faturaId()).mes()))
                .toList();
    }
}
