package dev.bolso.lancamentos.application;

import dev.bolso.compartilhado.UuidV7;
import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.DadosDoLancamento;
import dev.bolso.lancamentos.domain.Lancamento;
import dev.bolso.lancamentos.domain.LancamentoNaoEncontrado;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import dev.bolso.orcamento.OrcamentoApi;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Escritas: lançar, editar e excluir. Toda escrita confere a permissão no espaço primeiro. */
@Service
@Transactional
public class GravarLancamentos {

    private final LancamentosRepository lancamentos;
    private final TagsRepository tags;
    private final ConsultaDeLancamentos consulta;
    private final IdentidadeApi identidade;
    private final OrcamentoApi orcamento;
    private final Relogio relogio;

    public GravarLancamentos(
            LancamentosRepository lancamentos,
            TagsRepository tags,
            ConsultaDeLancamentos consulta,
            IdentidadeApi identidade,
            OrcamentoApi orcamento,
            Relogio relogio) {
        this.lancamentos = lancamentos;
        this.tags = tags;
        this.consulta = consulta;
        this.identidade = identidade;
        this.orcamento = orcamento;
        this.relogio = relogio;
    }

    public ItemDeLancamento lancar(UUID espacoId, DadosDoLancamento dados) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var lancamento = Lancamento.novo(UuidV7.gerar(), espacoId, identidade.tipoDoEspaco(espacoId), dados);
        conferirReferencias(lancamento);
        lancamentos.salvar(lancamento, relogio.agora());
        return itemGravado(lancamento);
    }

    /** {@code versaoLida} é a versão que a tela leu; se mudou, 409. */
    public ItemDeLancamento editar(UUID espacoId, UUID id, DadosDoLancamento dados, long versaoLida) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var atual = lancamentos.buscar(espacoId, id).orElseThrow(() -> new LancamentoNaoEncontrado(id));
        if (atual.versao() != versaoLida) {
            throw new ConflitoDeVersao(id);
        }
        var editado = atual.editar(identidade.tipoDoEspaco(espacoId), dados);
        conferirReferencias(editado);
        lancamentos.salvar(editado, relogio.agora());
        return itemGravado(editado);
    }

    public void excluir(UUID espacoId, UUID id) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        if (!lancamentos.excluir(espacoId, id, relogio.agora())) {
            throw new LancamentoNaoEncontrado(id);
        }
    }

    /** A meta e as tags precisam existir neste espaço (o banco também garante, por chave composta). */
    private void conferirReferencias(Lancamento lancamento) {
        if (lancamento.metaId() != null && !orcamento.metaExisteNoEspaco(lancamento.espacoId(), lancamento.metaId())) {
            throw new RegraDeNegocio("Meta não encontrada neste espaço");
        }
        if (!tags.todasAtivasNoEspaco(lancamento.espacoId(), lancamento.tagIds())) {
            throw new RegraDeNegocio("Tag não encontrada neste espaço");
        }
    }

    private ItemDeLancamento itemGravado(Lancamento lancamento) {
        return consulta.buscar(lancamento.espacoId(), lancamento.id()).orElseThrow();
    }
}
