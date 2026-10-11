package dev.bolso.lancamentos.adapter.out.persistence;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.application.ParcelamentosRepository;
import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.Parcelamento;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * O cabeçalho do parcelamento ({@code finance.installment_plan}) com JdbcClient: é uma linha pequena, que só muda
 * de descrição, e a versão é conferida no próprio {@code UPDATE}. As parcelas são lançamentos (JPA).
 */
@Repository
class JdbcParcelamentosRepository implements ParcelamentosRepository {

    private final JdbcClient jdbc;

    JdbcParcelamentosRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Parcelamento> buscar(UUID espacoId, UUID id) {
        return jdbc.sql("""
                SELECT id, workspace_id, card_id, description, total_amount, installments, first_installment,
                       purchase_date, version
                  FROM finance.installment_plan
                 WHERE id = :id AND workspace_id = :espaco AND deleted_at IS NULL
                """)
                .param("id", id)
                .param("espaco", espacoId)
                .query((rs, n) -> Parcelamento.reconstituir(
                        rs.getObject("id", UUID.class),
                        rs.getObject("workspace_id", UUID.class),
                        rs.getObject("card_id", UUID.class),
                        rs.getString("description"),
                        Dinheiro.deNumerico(rs.getBigDecimal("total_amount")),
                        rs.getInt("installments"),
                        rs.getInt("first_installment"),
                        rs.getObject("purchase_date", LocalDate.class),
                        rs.getLong("version")))
                .optional();
    }

    @Override
    public void criar(Parcelamento p, Instant agora) {
        jdbc.sql("""
                INSERT INTO finance.installment_plan
                    (id, workspace_id, card_id, description, total_amount, installments, first_installment, purchase_date,
                     created_at, updated_at)
                VALUES (:id, :espaco, :cartao, :descricao, :total, :parcelas, :atual, :data, :agora, :agora)
                """)
                .param("id", p.id())
                .param("espaco", p.espacoId())
                .param("cartao", p.cartaoId())
                .param("descricao", p.descricao())
                .param("total", p.valorTotal().valor())
                .param("parcelas", p.parcelas())
                .param("atual", p.parcelaAtual())
                .param("data", p.dataDaCompra())
                .param("agora", Timestamp.from(agora))
                .update();
    }

    @Override
    public void atualizar(Parcelamento p, Instant agora) {
        var linhas = jdbc.sql("""
                UPDATE finance.installment_plan
                   SET description = :descricao, updated_at = :agora, version = version + 1
                 WHERE id = :id AND workspace_id = :espaco AND deleted_at IS NULL AND version = :versao
                """)
                .param("descricao", p.descricao())
                .param("agora", Timestamp.from(agora))
                .param("id", p.id())
                .param("espaco", p.espacoId())
                .param("versao", p.versao())
                .update();
        if (linhas != 1) {
            throw new ConflitoDeVersao(p.id());
        }
    }

    @Override
    public void excluir(UUID espacoId, UUID id, Instant agora) {
        jdbc.sql("""
                UPDATE finance.installment_plan
                   SET deleted_at = :agora, updated_at = :agora, version = version + 1
                 WHERE id = :id AND workspace_id = :espaco AND deleted_at IS NULL
                """)
                .param("agora", Timestamp.from(agora))
                .param("id", id)
                .param("espaco", espacoId)
                .update();
    }
}
