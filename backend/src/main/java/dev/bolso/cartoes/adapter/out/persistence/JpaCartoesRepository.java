package dev.bolso.cartoes.adapter.out.persistence;

import dev.bolso.cartoes.application.CartoesRepository;
import dev.bolso.cartoes.domain.Cartao;
import dev.bolso.cartoes.domain.DadosDoCartao;
import dev.bolso.cartoes.domain.RegraDoCartao;
import dev.bolso.cartoes.domain.VersaoDesatualizada;
import dev.bolso.compartilhado.Dinheiro;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Cartões com JPA ({@code @Version} = locking otimista); a checagem de nome repetido é SQL ({@code lower}). */
@Repository
class JpaCartoesRepository implements CartoesRepository {

    @PersistenceContext
    private EntityManager em;

    private final JdbcClient jdbc;

    JpaCartoesRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Cartao> listarAtivos(UUID espacoId) {
        return em.createQuery(
                        "select c from CartaoEntity c where c.espacoId = :espaco and c.arquivadoEm is null order by lower(c.nome)",
                        CartaoEntity.class)
                .setParameter("espaco", espacoId)
                .getResultList()
                .stream()
                .map(JpaCartoesRepository::paraDominio)
                .toList();
    }

    @Override
    public Optional<Cartao> buscarAtivo(UUID espacoId, UUID id) {
        return ativo(espacoId, id).map(JpaCartoesRepository::paraDominio);
    }

    @Override
    public Optional<Cartao> buscarMesmoArquivado(UUID espacoId, UUID id) {
        return Optional.ofNullable(em.find(CartaoEntity.class, id))
                .filter(e -> e.espacoId.equals(espacoId))
                .map(JpaCartoesRepository::paraDominio);
    }

    @Override
    public boolean existeOutroAtivoComNome(UUID espacoId, String nome, UUID excetoId) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM finance.credit_card
                                WHERE workspace_id = :espaco AND archived_at IS NULL
                                  AND lower(name) = lower(:nome) AND id <> :exceto)
                """)
                .param("espaco", espacoId)
                .param("nome", nome)
                .param("exceto", excetoId == null ? new UUID(0, 0) : excetoId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public void salvar(Cartao c, Instant agora) {
        var entidade = em.find(CartaoEntity.class, c.id());
        var novo = entidade == null;
        if (novo) {
            entidade = new CartaoEntity();
            entidade.id = c.id();
            entidade.espacoId = c.espacoId();
            entidade.criadoEm = agora;
        }
        entidade.nome = c.nome();
        entidade.diaFechamento = c.diaFechamento();
        entidade.diaVencimento = c.diaVencimento();
        entidade.limite = c.limite().valor();
        entidade.atualizadoEm = agora;
        flush(novo ? entidade : null, c.id());
    }

    @Override
    public void arquivar(Cartao c, Instant agora) {
        var entidade = ativo(c.espacoId(), c.id()).orElseThrow();
        entidade.arquivadoEm = agora;
        entidade.atualizadoEm = agora;
        flush(null, c.id());
    }

    @Override
    public Optional<String> nomeDe(UUID espacoId, UUID id) {
        return jdbc.sql("SELECT name FROM finance.credit_card WHERE id = :id AND workspace_id = :espaco")
                .param("id", id)
                .param("espaco", espacoId)
                .query(String.class)
                .optional();
    }

    /** {@code atualizadoEm} sempre muda, então o {@code @Version} sobe sozinho no flush. */
    private void flush(CartaoEntity novo, UUID id) {
        try {
            if (novo != null) {
                em.persist(novo);
            }
            em.flush();
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            throw new VersaoDesatualizada(id);
        } catch (PersistenceException e) {
            // dois cadastros simultâneos com o mesmo nome passam pela conferência; o índice único segura
            if (String.valueOf(e.getMessage()).contains("ux_credit_card_workspace_name")
                    || String.valueOf(e.getCause()).contains("ux_credit_card_workspace_name")) {
                throw new RegraDoCartao("Já existe um cartão com esse nome");
            }
            throw e;
        }
    }

    private Optional<CartaoEntity> ativo(UUID espacoId, UUID id) {
        return em.createQuery(
                        "select c from CartaoEntity c where c.id = :id and c.espacoId = :espaco and c.arquivadoEm is null",
                        CartaoEntity.class)
                .setParameter("id", id)
                .setParameter("espaco", espacoId)
                .getResultStream()
                .findFirst();
    }

    private static Cartao paraDominio(CartaoEntity e) {
        return Cartao.reconstituir(
                e.id,
                e.espacoId,
                new DadosDoCartao(e.nome, e.diaFechamento, e.diaVencimento, Dinheiro.deNumerico(e.limite)),
                e.versao);
    }
}
