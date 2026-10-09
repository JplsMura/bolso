package dev.bolso.orcamento.adapter.out.persistence;

import dev.bolso.orcamento.application.MetasRepository;
import dev.bolso.orcamento.domain.Meta;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMetasRepository implements MetasRepository {

    private final JdbcClient jdbc;

    JdbcMetasRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Meta> listarDoEspaco(UUID espacoId) {
        return jdbc.sql("""
                SELECT id, name, color, sort_order
                  FROM finance.budget_goal
                 WHERE workspace_id = :espaco
                 ORDER BY sort_order
                """)
                .param("espaco", espacoId)
                .query((rs, n) -> new Meta(
                        rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("color"), rs.getInt("sort_order")))
                .list();
    }

    @Override
    public boolean existeNoEspaco(UUID espacoId, UUID metaId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM finance.budget_goal WHERE id = :meta AND workspace_id = :espaco)")
                .param("meta", metaId)
                .param("espaco", espacoId)
                .query(Boolean.class)
                .single();
    }
}
