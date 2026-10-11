package dev.bolso.cartoes.adapter.out.persistence;

import dev.bolso.cartoes.application.FaturasRepository;
import dev.bolso.cartoes.domain.Fatura;
import dev.bolso.cartoes.domain.StatusDaFatura;
import dev.bolso.cartoes.domain.VersaoDesatualizada;
import dev.bolso.compartilhado.Dinheiro;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Faturas. Leitura e criação com SQL direto (a criação usa {@code ON CONFLICT DO NOTHING} e trava a linha para
 * leitura); mudança de status com JPA, travando a linha para escrita e conferindo o {@code @Version}.
 */
@Repository
class JpaFaturasRepository implements FaturasRepository {

    private static final String COLUNAS = """
            SELECT id, workspace_id, card_id, reference_month, closing_date, due_date, status, total_amount, paid_on, version
              FROM finance.card_invoice
            """;

    @PersistenceContext
    private EntityManager em;

    private final JdbcClient jdbc;

    JpaFaturasRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Fatura> buscar(UUID espacoId, UUID id) {
        return jdbc.sql(COLUNAS + " WHERE id = :id AND workspace_id = :espaco")
                .param("id", id)
                .param("espaco", espacoId)
                .query((rs, n) -> linha(rs))
                .optional();
    }

    @Override
    public Optional<Fatura> buscarParaMudar(UUID espacoId, UUID id) {
        var entidade = em.find(FaturaEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entidade == null || !entidade.espacoId.equals(espacoId)) {
            return Optional.empty();
        }
        return Optional.of(paraDominio(entidade));
    }

    @Override
    public void salvar(Fatura f, Instant agora) {
        var entidade = em.find(FaturaEntity.class, f.id());
        entidade.status = f.status().name();
        entidade.total = f.totalCongelado() == null ? null : f.totalCongelado().valor();
        entidade.pagaEm = f.pagaEm();
        entidade.atualizadoEm = agora;
        try {
            em.flush(); // atualizadoEm sempre muda: o @Version sobe sozinho
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            throw new VersaoDesatualizada(f.id());
        }
    }

    @Override
    public Fatura obterOuCriar(Fatura nova) {
        jdbc.sql("""
                INSERT INTO finance.card_invoice (id, workspace_id, card_id, reference_month, closing_date, due_date)
                VALUES (:id, :espaco, :cartao, :mes, :fechamento, :vencimento)
                ON CONFLICT (card_id, reference_month) DO NOTHING
                """)
                .param("id", nova.id())
                .param("espaco", nova.espacoId())
                .param("cartao", nova.cartaoId())
                .param("mes", nova.mes().atDay(1))
                .param("fechamento", nova.fechamento())
                .param("vencimento", nova.vencimento())
                .update();
        return jdbc.sql(COLUNAS + " WHERE card_id = :cartao AND reference_month = :mes AND workspace_id = :espaco FOR SHARE")
                .param("cartao", nova.cartaoId())
                .param("mes", nova.mes().atDay(1))
                .param("espaco", nova.espacoId())
                .query((rs, n) -> linha(rs))
                .single();
    }

    @Override
    public Optional<Fatura> doCartaoNoMes(UUID espacoId, UUID cartaoId, YearMonth mes) {
        return jdbc.sql(COLUNAS + " WHERE card_id = :cartao AND reference_month = :mes AND workspace_id = :espaco")
                .param("cartao", cartaoId)
                .param("mes", mes.atDay(1))
                .param("espaco", espacoId)
                .query((rs, n) -> linha(rs))
                .optional();
    }

    @Override
    public List<Fatura> porIds(UUID espacoId, Collection<UUID> ids, boolean travar) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(COLUNAS + " WHERE workspace_id = :espaco AND id IN (:ids)" + (travar ? " FOR SHARE" : ""))
                .param("espaco", espacoId)
                .param("ids", ids)
                .query((rs, n) -> linha(rs))
                .list();
    }

    @Override
    public List<Fatura> doMes(UUID espacoId, YearMonth mes) {
        return jdbc.sql(COLUNAS + " WHERE workspace_id = :espaco AND reference_month = :mes")
                .param("espaco", espacoId)
                .param("mes", mes.atDay(1))
                .query((rs, n) -> linha(rs))
                .list();
    }

    @Override
    public List<Fatura> naoPagas(UUID espacoId) {
        return jdbc.sql(COLUNAS + " WHERE workspace_id = :espaco AND status <> 'PAID'")
                .param("espaco", espacoId)
                .query((rs, n) -> linha(rs))
                .list();
    }

    private static Fatura linha(ResultSet rs) throws SQLException {
        return Fatura.reconstituir(
                rs.getObject("id", UUID.class),
                rs.getObject("workspace_id", UUID.class),
                rs.getObject("card_id", UUID.class),
                YearMonth.from(rs.getObject("reference_month", LocalDate.class)),
                rs.getObject("closing_date", LocalDate.class),
                rs.getObject("due_date", LocalDate.class),
                StatusDaFatura.valueOf(rs.getString("status")),
                dinheiro(rs.getBigDecimal("total_amount")),
                rs.getObject("paid_on", LocalDate.class),
                rs.getLong("version"));
    }

    private static Fatura paraDominio(FaturaEntity e) {
        return Fatura.reconstituir(
                e.id, e.espacoId, e.cartaoId, YearMonth.from(e.mes), e.fechamento, e.vencimento,
                StatusDaFatura.valueOf(e.status), dinheiro(e.total), e.pagaEm, e.versao);
    }

    private static Dinheiro dinheiro(BigDecimal valor) {
        return valor == null ? null : Dinheiro.deNumerico(valor);
    }
}
