package dev.bolso.lancamentos.adapter.out.persistence;

import dev.bolso.lancamentos.application.TagsRepository;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import dev.bolso.lancamentos.domain.Tag;
import dev.bolso.lancamentos.domain.TipoCusto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcTagsRepository implements TagsRepository {

    private final JdbcClient jdbc;

    JdbcTagsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Tag> listarAtivas(UUID espacoId) {
        return jdbc.sql("""
                SELECT id, name::text AS name, cost_type
                  FROM finance.tag
                 WHERE workspace_id = :espaco AND archived_at IS NULL
                 ORDER BY name
                """)
                .param("espaco", espacoId)
                .query((rs, n) -> new Tag(
                        rs.getObject("id", UUID.class),
                        espacoId,
                        rs.getString("name"),
                        rs.getString("cost_type") == null ? null : TipoCusto.valueOf(rs.getString("cost_type"))))
                .list();
    }

    @Override
    public boolean existeNome(UUID espacoId, String nome) {
        // CAST para citext: comparar citext com texto simples diferenciaria maiúsculas
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM finance.tag WHERE workspace_id = :espaco AND name = CAST(:nome AS citext))")
                .param("espaco", espacoId)
                .param("nome", nome)
                .query(Boolean.class)
                .single();
    }

    @Override
    public void salvar(Tag tag) {
        try {
            jdbc.sql("INSERT INTO finance.tag (id, workspace_id, name, cost_type) VALUES (:id, :espaco, :nome, :tipo)")
                    .param("id", tag.id())
                    .param("espaco", tag.espacoId())
                    .param("nome", tag.nome())
                    .param("tipo", tag.tipoCusto() == null ? null : tag.tipoCusto().name())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new RegraDeNegocio("Já existe uma tag com o nome " + tag.nome());
        }
    }

    @Override
    public boolean todasAtivasNoEspaco(UUID espacoId, Set<UUID> tagIds) {
        if (tagIds.isEmpty()) {
            return true;
        }
        var encontradas = jdbc.sql("""
                SELECT count(*) FROM finance.tag
                 WHERE workspace_id = :espaco AND archived_at IS NULL AND id IN (:ids)
                """)
                .param("espaco", espacoId)
                .param("ids", tagIds)
                .query(Long.class)
                .single();
        return encontradas == tagIds.size();
    }
}
