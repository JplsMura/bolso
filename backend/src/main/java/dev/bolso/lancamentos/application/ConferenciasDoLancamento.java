package dev.bolso.lancamentos.application;

import dev.bolso.lancamentos.FaturaResolvida;
import dev.bolso.lancamentos.FaturasDoCartao;
import dev.bolso.lancamentos.domain.FaturaFechada;
import dev.bolso.lancamentos.domain.Lancamento;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import dev.bolso.orcamento.OrcamentoApi;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * O que os casos de uso de escrita conferem fora do domínio: meta e tags do espaço, e a fatura em que o gasto no
 * cartão cai (que precisa estar aberta). O banco também garante as referências, por chave composta.
 */
@Component
public class ConferenciasDoLancamento {

    private final TagsRepository tags;
    private final OrcamentoApi orcamento;
    private final FaturasDoCartao faturas;

    public ConferenciasDoLancamento(TagsRepository tags, OrcamentoApi orcamento, FaturasDoCartao faturas) {
        this.tags = tags;
        this.orcamento = orcamento;
        this.faturas = faturas;
    }

    /** A meta e as tags precisam existir neste espaço. */
    public void conferirReferencias(Lancamento lancamento) {
        if (lancamento.metaId() != null && !orcamento.metaExisteNoEspaco(lancamento.espacoId(), lancamento.metaId())) {
            throw new RegraDeNegocio("Meta não encontrada neste espaço");
        }
        if (!tags.todasAtivasNoEspaco(lancamento.espacoId(), lancamento.tagIds())) {
            throw new RegraDeNegocio("Tag não encontrada neste espaço");
        }
    }

    /** Mês da fatura em que cai uma compra nesta data. Recusa cartão de outro espaço ou arquivado. */
    public YearMonth mesDaFatura(UUID espacoId, UUID cartaoId, LocalDate data) {
        return faturas.mesDaFaturaDaCompra(espacoId, cartaoId, data)
                .orElseThrow(() -> new RegraDeNegocio("Cartão não encontrado neste espaço"));
    }

    /** A fatura do mês (criada se preciso), que precisa estar aberta. */
    public FaturaResolvida faturaAberta(UUID espacoId, UUID cartaoId, YearMonth mes) {
        return exigirAberta(faturas.faturaDoMes(espacoId, cartaoId, mes));
    }

    /** A fatura em que o gasto já está precisa estar aberta para ele mudar de valor, data, cartão ou sair. */
    public void exigirAberta(UUID espacoId, UUID faturaId) {
        exigirAberta(faturas.faturaTravada(espacoId, faturaId)
                .orElseThrow(() -> new IllegalStateException("Fatura " + faturaId + " do lançamento não encontrada")));
    }

    /** A fatura (travada para leitura) ainda aceita mudanças? */
    public boolean estaAberta(UUID espacoId, UUID faturaId) {
        return faturas.faturaTravada(espacoId, faturaId).map(FaturaResolvida::aberta).orElse(false);
    }

    private static FaturaResolvida exigirAberta(FaturaResolvida fatura) {
        if (!fatura.aberta()) {
            throw new FaturaFechada(fatura.nomeDoCartao(), fatura.mes());
        }
        return fatura;
    }
}
