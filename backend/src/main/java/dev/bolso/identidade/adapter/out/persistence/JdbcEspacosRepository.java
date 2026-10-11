package dev.bolso.identidade.adapter.out.persistence;

import dev.bolso.identidade.Papel;
import dev.bolso.identidade.TipoEspaco;
import dev.bolso.identidade.application.EspacosRepository;
import dev.bolso.identidade.domain.Espaco;
import dev.bolso.identidade.domain.UsuarioId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Leitura dos espaços com JdbcClient: só o que o usuário tem vínculo. */
@Repository
class JdbcEspacosRepository implements EspacosRepository {

    private static final String SELECT = """
            SELECT w.id, w.kind, w.name, m.role
              FROM identity.workspace w
              JOIN identity.workspace_member m ON m.workspace_id = w.id
             WHERE m.user_id = :usuario
            """;

    private static final RowMapper<Espaco> MAPEADOR = (rs, n) -> new Espaco(
            rs.getObject("id", UUID.class),
            TipoEspaco.valueOf(rs.getString("kind")),
            rs.getString("name"),
            Papel.valueOf(rs.getString("role")));

    private final JdbcClient jdbc;

    JdbcEspacosRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Espaco> listarDoUsuario(UsuarioId usuario) {
        return jdbc.sql(SELECT).param("usuario", usuario.valor()).query(MAPEADOR).list();
    }

    @Override
    public Optional<Espaco> buscarDoUsuario(UsuarioId usuario, UUID espacoId) {
        return jdbc.sql(SELECT + " AND w.id = :espaco")
                .param("usuario", usuario.valor())
                .param("espaco", espacoId)
                .query(MAPEADOR)
                .optional();
    }
}
