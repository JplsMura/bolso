package dev.bolso.lancamentos.application;

import dev.bolso.compartilhado.UuidV7;
import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.DadosDoLancamento;
import dev.bolso.lancamentos.domain.Lancamento;
import dev.bolso.lancamentos.domain.LancamentoNaoEncontrado;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escritas: lançar, editar e excluir. Toda escrita confere a permissão no espaço primeiro. Gasto no cartão cai numa
 * fatura aberta; mexer no valor, data, cartão ou forma de pagamento, ou excluir, exige que a fatura atual esteja
 * aberta. Parcela se edita pelo parcelamento.
 */
@Service
@Transactional
public class GravarLancamentos {

    static final String PARCELA_PELO_PARCELAMENTO = "Este gasto é uma parcela: edite ou exclua pelo parcelamento";

    private final LancamentosRepository lancamentos;
    private final ConsultarLancamentos consultar;
    private final ConferenciasDoLancamento conferencias;
    private final IdentidadeApi identidade;
    private final Relogio relogio;

    public GravarLancamentos(
            LancamentosRepository lancamentos,
            ConsultarLancamentos consultar,
            ConferenciasDoLancamento conferencias,
            IdentidadeApi identidade,
            Relogio relogio) {
        this.lancamentos = lancamentos;
        this.consultar = consultar;
        this.conferencias = conferencias;
        this.identidade = identidade;
        this.relogio = relogio;
    }

    public ItemDeLancamento lancar(UUID espacoId, DadosDoLancamento dados) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var lancamento = Lancamento.novo(UuidV7.gerar(), espacoId, identidade.tipoDoEspaco(espacoId), dados);
        conferencias.conferirReferencias(lancamento);
        if (lancamento.cartaoId() != null) {
            lancamento = lancamento.naFatura(faturaDaCompra(lancamento));
        }
        lancamentos.salvar(lancamento, relogio.agora());
        return consultar.buscar(espacoId, lancamento.id());
    }

    /** {@code versaoLida} é a versão que a tela leu; se mudou, 409. */
    public ItemDeLancamento editar(UUID espacoId, UUID id, DadosDoLancamento dados, long versaoLida) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var atual = lancamentos.buscar(espacoId, id).orElseThrow(() -> new LancamentoNaoEncontrado(id));
        if (atual.versao() != versaoLida) {
            throw new ConflitoDeVersao(id);
        }
        if (atual.ehParcela()) {
            throw new RegraDeNegocio(PARCELA_PELO_PARCELAMENTO);
        }
        var editado = atual.editar(identidade.tipoDoEspaco(espacoId), dados);
        conferencias.conferirReferencias(editado);
        if (editado.mexeNaFaturaEmRelacaoA(atual)) {
            if (atual.faturaId() != null) {
                conferencias.exigirAberta(espacoId, atual.faturaId());
            }
            if (editado.cartaoId() != null) {
                editado = editado.naFatura(faturaDaCompra(editado));
            }
        }
        lancamentos.salvar(editado, relogio.agora());
        return consultar.buscar(espacoId, id);
    }

    public void excluir(UUID espacoId, UUID id) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var atual = lancamentos.buscar(espacoId, id).orElseThrow(() -> new LancamentoNaoEncontrado(id));
        if (atual.ehParcela()) {
            throw new RegraDeNegocio(PARCELA_PELO_PARCELAMENTO);
        }
        if (atual.faturaId() != null) {
            conferencias.exigirAberta(espacoId, atual.faturaId());
        }
        if (!lancamentos.excluir(espacoId, id, relogio.agora())) {
            throw new LancamentoNaoEncontrado(id);
        }
    }

    private UUID faturaDaCompra(Lancamento lancamento) {
        var mes = conferencias.mesDaFatura(lancamento.espacoId(), lancamento.cartaoId(), lancamento.data());
        return conferencias.faturaAberta(lancamento.espacoId(), lancamento.cartaoId(), mes).id();
    }
}
