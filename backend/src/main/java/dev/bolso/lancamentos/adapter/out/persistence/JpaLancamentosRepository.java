package dev.bolso.lancamentos.adapter.out.persistence;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.application.LancamentosRepository;
import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.DadosDoLancamento;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import dev.bolso.lancamentos.domain.Lancamento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Gravação do agregado com JPA ({@code @Version} = locking otimista). As tags (tabela de ligação com
 * {@code workspace_id}) são regravadas com JdbcClient na mesma transação.
 */
@Repository
class JpaLancamentosRepository implements LancamentosRepository {

    @PersistenceContext
    private EntityManager em;

    private final JdbcClient jdbc;

    JpaLancamentosRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Lancamento> buscar(UUID espacoId, UUID id) {
        return naoExcluida(espacoId, id).map(this::paraDominio);
    }

    @Override
    public void salvar(Lancamento l, Instant agora) {
        var entidade = em.find(LancamentoEntity.class, l.id());
        var nova = entidade == null;
        if (nova) {
            entidade = new LancamentoEntity();
            entidade.id = l.id();
            entidade.espacoId = l.espacoId();
            entidade.criadoEm = agora;
        }
        entidade.direcao = l.direcao().name();
        entidade.valor = l.valor().valor();
        entidade.descricao = l.descricao();
        entidade.data = l.data();
        entidade.mesReferencia = l.mesReferencia().atDay(1);
        entidade.metaId = l.metaId();
        entidade.formaPagamento = l.formaPagamento() == null ? null : l.formaPagamento().name();
        entidade.atualizadoEm = agora;

        try {
            // Toda gravação muda atualizadoEm, então o @Version sobe sozinho no flush (inclusive quando só as
            // tags mudaram). Forçar o incremento por cima faria a versão subir duas vezes.
            if (nova) {
                em.persist(entidade);
            }
            em.flush();
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            throw new ConflitoDeVersao(l.id());
        }
        regravarTags(l);
    }

    @Override
    public boolean excluir(UUID espacoId, UUID id, Instant agora) {
        var encontrada = naoExcluida(espacoId, id);
        if (encontrada.isEmpty()) {
            return false;
        }
        var entidade = encontrada.get();
        entidade.excluidoEm = agora;
        entidade.atualizadoEm = agora;
        em.flush();
        return true;
    }

    private Optional<LancamentoEntity> naoExcluida(UUID espacoId, UUID id) {
        return em.createQuery(
                        "select l from LancamentoEntity l where l.id = :id and l.espacoId = :espaco and l.excluidoEm is null",
                        LancamentoEntity.class)
                .setParameter("id", id)
                .setParameter("espaco", espacoId)
                .getResultStream()
                .findFirst();
    }

    private Lancamento paraDominio(LancamentoEntity e) {
        Set<UUID> tagIds = new HashSet<>(jdbc.sql("SELECT tag_id FROM finance.transaction_tag WHERE transaction_id = :id")
                .param("id", e.id)
                .query(UUID.class)
                .list());
        var dados = new DadosDoLancamento(
                Direcao.valueOf(e.direcao),
                Dinheiro.deNumerico(e.valor),
                e.descricao,
                e.data,
                e.metaId,
                e.formaPagamento == null ? null : FormaPagamento.valueOf(e.formaPagamento),
                tagIds);
        return Lancamento.reconstituir(e.id, e.espacoId, dados, e.versao);
    }

    private void regravarTags(Lancamento l) {
        jdbc.sql("DELETE FROM finance.transaction_tag WHERE transaction_id = :id").param("id", l.id()).update();
        List<UUID> tagIds = List.copyOf(l.tagIds());
        for (var tagId : tagIds) {
            jdbc.sql("INSERT INTO finance.transaction_tag (transaction_id, tag_id, workspace_id) VALUES (:t, :g, :w)")
                    .param("t", l.id())
                    .param("g", tagId)
                    .param("w", l.espacoId())
                    .update();
        }
    }
}
