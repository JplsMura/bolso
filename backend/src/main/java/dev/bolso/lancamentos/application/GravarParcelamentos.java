package dev.bolso.lancamentos.application;

import dev.bolso.compartilhado.UuidV7;
import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.DadosDoLancamento;
import dev.bolso.lancamentos.domain.DadosDoParcelamento;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import dev.bolso.lancamentos.domain.Lancamento;
import dev.bolso.lancamentos.domain.Parcela;
import dev.bolso.lancamentos.domain.Parcelamento;
import dev.bolso.lancamentos.domain.ParcelamentoNaoEncontrado;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compra parcelada no cartão: cria as parcelas (uma por fatura, todas abertas), edita descrição, meta e tags das
 * parcelas em faturas abertas, e exclui as parcelas em faturas abertas. As de faturas fechadas ou pagas ficam.
 */
@Service
@Transactional
public class GravarParcelamentos {

    static final String NADA_ABERTO = "Todas as parcelas que sobram estão em faturas fechadas ou pagas";

    private final ParcelamentosRepository parcelamentos;
    private final LancamentosRepository lancamentos;
    private final ConsultarParcelamentos consultar;
    private final ConferenciasDoLancamento conferencias;
    private final IdentidadeApi identidade;
    private final Relogio relogio;

    public GravarParcelamentos(
            ParcelamentosRepository parcelamentos,
            LancamentosRepository lancamentos,
            ConsultarParcelamentos consultar,
            ConferenciasDoLancamento conferencias,
            IdentidadeApi identidade,
            Relogio relogio) {
        this.parcelamentos = parcelamentos;
        this.lancamentos = lancamentos;
        this.consultar = consultar;
        this.conferencias = conferencias;
        this.identidade = identidade;
        this.relogio = relogio;
    }

    public VisaoDoParcelamento criar(UUID espacoId, DadosDoParcelamento dados) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var tipo = identidade.tipoDoEspaco(espacoId);
        var parcelamento = Parcelamento.novo(UuidV7.gerar(), espacoId, dados);

        // cada parcela passa pelas regras de um lançamento (meta conforme o espaço, tags, descrição)
        var planejadas = parcelamento.parcelasACriar();
        var novas = new ArrayList<Lancamento>();
        for (var p : planejadas) {
            var dadosDaParcela = new DadosDoLancamento(
                    Direcao.OUT, p.valor(), parcelamento.descricao(), p.data(), dados.metaId(), FormaPagamento.CREDIT,
                    parcelamento.cartaoId(), dados.tagIds() == null ? Set.of() : dados.tagIds());
            novas.add(Lancamento.novo(UuidV7.gerar(), espacoId, tipo, dadosDaParcela));
        }
        conferencias.conferirReferencias(novas.getFirst());

        var mesDaPrimeira = conferencias.mesDaFatura(espacoId, parcelamento.cartaoId(), parcelamento.dataDaCompra());
        var agora = relogio.agora();
        parcelamentos.criar(parcelamento, agora);
        for (var i = 0; i < novas.size(); i++) {
            var p = planejadas.get(i);
            var fatura = conferencias.faturaAberta(
                    espacoId, parcelamento.cartaoId(), mesDaPrimeira.plusMonths(p.mesesDepoisDaPrimeira()));
            var parcela = novas.get(i)
                    .naFatura(fatura.id())
                    .comoParcela(new Parcela(parcelamento.id(), p.numero(), parcelamento.parcelas()));
            lancamentos.salvar(parcela, agora);
        }
        return consultar.buscar(espacoId, parcelamento.id());
    }

    /** Muda descrição, meta e tags das parcelas em faturas abertas. {@code versaoLida} é a do parcelamento. */
    public VisaoDoParcelamento editar(
            UUID espacoId, UUID id, String descricao, UUID metaId, Set<UUID> tagIds, long versaoLida) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var tipo = identidade.tipoDoEspaco(espacoId);
        var atual = parcelamentos.buscar(espacoId, id).orElseThrow(() -> new ParcelamentoNaoEncontrado(id));
        if (atual.versao() != versaoLida) {
            throw new ConflitoDeVersao(id);
        }
        var editado = atual.comDescricao(descricao);
        var abertas = parcelasEmFaturaAberta(espacoId, id);
        if (abertas.isEmpty()) {
            throw new RegraDeNegocio(NADA_ABERTO);
        }
        var editadas = abertas.stream()
                .map(l -> l.editar(tipo, new DadosDoLancamento(
                        Direcao.OUT, l.valor(), editado.descricao(), l.data(), metaId, FormaPagamento.CREDIT, l.cartaoId(),
                        tagIds == null ? Set.of() : tagIds)))
                .toList();
        conferencias.conferirReferencias(editadas.getFirst());

        var agora = relogio.agora();
        parcelamentos.atualizar(editado, agora);
        editadas.forEach(l -> lancamentos.salvar(l, agora));
        return consultar.buscar(espacoId, id);
    }

    /** Exclui as parcelas em faturas abertas; o parcelamento some quando não sobra nenhuma. */
    public void excluir(UUID espacoId, UUID id) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        parcelamentos.buscar(espacoId, id).orElseThrow(() -> new ParcelamentoNaoEncontrado(id));
        var todas = lancamentos.parcelasDe(espacoId, id);
        var abertas = parcelasEmFaturaAberta(espacoId, id);
        if (abertas.isEmpty()) {
            throw new RegraDeNegocio(NADA_ABERTO);
        }
        var agora = relogio.agora();
        abertas.forEach(l -> lancamentos.excluir(espacoId, l.id(), agora));
        if (abertas.size() == todas.size()) {
            parcelamentos.excluir(espacoId, id, agora);
        }
    }

    private List<Lancamento> parcelasEmFaturaAberta(UUID espacoId, UUID parcelamentoId) {
        Map<UUID, Boolean> aberta = new HashMap<>();
        return lancamentos.parcelasDe(espacoId, parcelamentoId).stream()
                .filter(l -> aberta.computeIfAbsent(l.faturaId(), f -> conferencias.estaAberta(espacoId, f)))
                .toList();
    }
}
