package dev.bolso.lancamentos.adapter.out.persistence;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.ItemDaFatura;
import dev.bolso.lancamentos.application.ConsultaDeLancamentos;
import dev.bolso.lancamentos.application.ConsultaDoMes;
import dev.bolso.lancamentos.application.FiltrosDeLancamento;
import dev.bolso.lancamentos.application.ItemDeLancamento;
import dev.bolso.lancamentos.application.TagResumo;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import dev.bolso.lancamentos.domain.Parcela;
import dev.bolso.lancamentos.domain.TotaisDoMes;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Leituras com SQL direto, sem carregar agregados: a lista do mês (com filtros e busca), os totais, as parcelas de um
 * parcelamento e, para o módulo Cartões, a soma e os itens de cada fatura.
 */
@Repository
class JdbcConsultaDeLancamentos implements ConsultaDeLancamentos {

    private static final String ITENS = """
            SELECT t.id, t.direction, t.amount, t.description, t.occurred_on, t.reference_month,
                   t.goal_id, t.payment_method, t.card_id, t.invoice_id, t.installment_plan_id, t.installment_no,
                   p.installments, t.version
              FROM finance.transaction t
              LEFT JOIN finance.installment_plan p ON p.id = t.installment_plan_id AND p.workspace_id = t.workspace_id
             WHERE t.workspace_id = :espaco AND t.deleted_at IS NULL
            """;

    private static final String TAGS = """
            SELECT tt.transaction_id, g.id AS tag_id, g.name::text AS tag_name
              FROM finance.transaction_tag tt
              JOIN finance.tag g ON g.id = tt.tag_id AND g.workspace_id = tt.workspace_id
              JOIN finance.transaction t ON t.id = tt.transaction_id AND t.workspace_id = tt.workspace_id
             WHERE t.workspace_id = :espaco AND t.deleted_at IS NULL
            """;

    private static final String TOTAIS = """
            SELECT COALESCE(SUM(amount) FILTER (WHERE direction = 'IN'), 0.00)  AS entradas,
                   COALESCE(SUM(amount) FILTER (WHERE direction = 'OUT'), 0.00) AS saidas
              FROM finance.transaction
             WHERE workspace_id = :espaco AND deleted_at IS NULL AND reference_month = :mes
            """;

    private final JdbcClient jdbc;

    JdbcConsultaDeLancamentos(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ConsultaDoMes consultarMes(UUID espacoId, YearMonth mes, FiltrosDeLancamento filtros) {
        var sql = new StringBuilder(ITENS).append(" AND t.reference_month = :mes\n");
        Map<String, Object> params = new HashMap<>();
        params.put("espaco", espacoId);
        params.put("mes", mes.atDay(1));

        if (filtros.direcao() != null) {
            sql.append(" AND t.direction = :direcao\n");
            params.put("direcao", filtros.direcao().name());
        }
        if (filtros.formaPagamento() != null) {
            sql.append(" AND t.payment_method = :pagamento\n");
            params.put("pagamento", filtros.formaPagamento().name());
        }
        if (filtros.cartaoId() != null) {
            sql.append(" AND t.card_id = :cartao\n");
            params.put("cartao", filtros.cartaoId());
        }
        if (filtros.metaId() != null) {
            sql.append(" AND t.goal_id = :meta\n");
            params.put("meta", filtros.metaId());
        }
        if (filtros.tagId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM finance.transaction_tag f WHERE f.transaction_id = t.id AND f.tag_id = :tag)\n");
            params.put("tag", filtros.tagId());
        }
        if (filtros.texto() != null) {
            sql.append("""
                     AND (t.description ILIKE :texto ESCAPE '\\'
                          OR EXISTS (SELECT 1
                                       FROM finance.transaction_tag q
                                       JOIN finance.tag g ON g.id = q.tag_id AND g.workspace_id = q.workspace_id
                                      WHERE q.transaction_id = t.id AND g.name::text ILIKE :texto ESCAPE '\\'))
                    """);
            params.put("texto", "%" + escaparCuringas(filtros.texto()) + "%");
        }
        sql.append(" ORDER BY t.occurred_on DESC, t.created_at DESC, t.id DESC");

        var itens = jdbc.sql(sql.toString()).params(params).query((rs, n) -> linha(rs)).list();
        var tags = tagsPorLancamento(TAGS + " AND t.reference_month = :mes\n", Map.of("espaco", espacoId, "mes", mes.atDay(1)));
        var comTags = itens.stream().map(i -> comTags(i, tags)).toList();

        var totais = jdbc.sql(TOTAIS)
                .param("espaco", espacoId)
                .param("mes", mes.atDay(1))
                .query((rs, n) -> new TotaisDoMes(
                        Dinheiro.deNumerico(rs.getBigDecimal("entradas")), Dinheiro.deNumerico(rs.getBigDecimal("saidas"))))
                .single();
        return new ConsultaDoMes(mes, totais, comTags);
    }

    @Override
    public Optional<ItemDeLancamento> buscar(UUID espacoId, UUID id) {
        return jdbc.sql(ITENS + " AND t.id = :id")
                .param("espaco", espacoId)
                .param("id", id)
                .query((rs, n) -> linha(rs))
                .optional()
                .map(item -> comTags(item, tagsPorLancamento(TAGS + " AND t.id = :id\n", Map.of("espaco", espacoId, "id", id))));
    }

    @Override
    public List<ItemDeLancamento> parcelasDoParcelamento(UUID espacoId, UUID parcelamentoId) {
        var itens = jdbc.sql(ITENS + " AND t.installment_plan_id = :parcelamento ORDER BY t.installment_no")
                .param("espaco", espacoId)
                .param("parcelamento", parcelamentoId)
                .query((rs, n) -> linha(rs))
                .list();
        var tags = tagsPorLancamento(
                TAGS + " AND t.installment_plan_id = :parcelamento\n", Map.of("espaco", espacoId, "parcelamento", parcelamentoId));
        return itens.stream().map(i -> comTags(i, tags)).toList();
    }

    @Override
    public Map<UUID, Dinheiro> somaPorFatura(UUID espacoId, Collection<UUID> faturaIds) {
        Map<UUID, Dinheiro> somas = new HashMap<>();
        faturaIds.forEach(id -> somas.put(id, Dinheiro.ZERO));
        jdbc.sql("""
                SELECT invoice_id, SUM(amount) AS total
                  FROM finance.transaction
                 WHERE workspace_id = :espaco AND deleted_at IS NULL AND invoice_id IN (:faturas)
                 GROUP BY invoice_id
                """)
                .param("espaco", espacoId)
                .param("faturas", faturaIds)
                .query((rs, n) -> somas.put(rs.getObject("invoice_id", UUID.class), Dinheiro.deNumerico(rs.getBigDecimal("total"))))
                .list();
        return somas;
    }

    @Override
    public List<ItemDaFatura> itensDasFaturas(UUID espacoId, Collection<UUID> faturaIds) {
        return jdbc.sql(ITENS + " AND t.invoice_id IN (:faturas) ORDER BY t.occurred_on DESC, t.created_at DESC, t.id DESC")
                .param("espaco", espacoId)
                .param("faturas", faturaIds)
                .query((rs, n) -> new ItemDaFatura(
                        rs.getObject("invoice_id", UUID.class),
                        rs.getObject("id", UUID.class),
                        rs.getString("description"),
                        rs.getObject("occurred_on", LocalDate.class),
                        rs.getObject("goal_id", UUID.class),
                        Dinheiro.deNumerico(rs.getBigDecimal("amount")),
                        (Integer) rs.getObject("installment_no"),
                        (Integer) rs.getObject("installments")))
                .list();
    }

    private static ItemDeLancamento linha(java.sql.ResultSet rs) throws java.sql.SQLException {
        var forma = rs.getString("payment_method");
        var parcelamento = rs.getObject("installment_plan_id", UUID.class);
        return new ItemDeLancamento(
                rs.getObject("id", UUID.class),
                Direcao.valueOf(rs.getString("direction")),
                Dinheiro.deNumerico(rs.getBigDecimal("amount")),
                rs.getString("description"),
                rs.getObject("occurred_on", LocalDate.class),
                YearMonth.from(rs.getObject("reference_month", LocalDate.class)),
                rs.getObject("goal_id", UUID.class),
                forma == null ? null : FormaPagamento.valueOf(forma),
                rs.getObject("card_id", UUID.class),
                rs.getObject("invoice_id", UUID.class),
                null,
                parcelamento == null
                        ? null
                        : new Parcela(parcelamento, rs.getInt("installment_no"), rs.getInt("installments")),
                List.of(),
                rs.getLong("version"));
    }

    private Map<UUID, List<TagResumo>> tagsPorLancamento(String sql, Map<String, Object> params) {
        Map<UUID, List<TagResumo>> porLancamento = new HashMap<>();
        jdbc.sql(sql + " ORDER BY g.name").params(params).query((rs, n) -> {
            porLancamento
                    .computeIfAbsent(rs.getObject("transaction_id", UUID.class), k -> new ArrayList<>())
                    .add(new TagResumo(rs.getObject("tag_id", UUID.class), rs.getString("tag_name")));
            return 0;
        }).list();
        return porLancamento;
    }

    private static ItemDeLancamento comTags(ItemDeLancamento i, Map<UUID, List<TagResumo>> tags) {
        return i.comTags(tags.getOrDefault(i.id(), List.of()));
    }

    /** % e _ digitados valem como texto, não como curinga do LIKE. */
    private static String escaparCuringas(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
