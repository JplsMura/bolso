package dev.bolso.identidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.TestcontainersConfig;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** V2 e V3 num PostgreSQL vazio: tabelas, dono local, espaços e restrições do banco. */
@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class IdentidadeMigracoesIT {

    static final String DONO = "019a0000-0000-7000-8000-000000000001";
    static final String CASA = "019a0000-0000-7000-8000-000000000011";
    static final String EMPRESA = "019a0000-0000-7000-8000-000000000012";

    @Autowired
    JdbcClient jdbc;

    @Test
    void criaAsTresTabelas() {
        var tabelas = jdbc.sql("SELECT table_name FROM information_schema.tables WHERE table_schema = 'identity'")
                .query(String.class)
                .list();

        assertThat(tabelas).contains("app_user", "workspace", "workspace_member");
    }

    @Test
    void existeUmDonoLocalAtivo() {
        var donos = jdbc.sql("SELECT id::text AS id, status FROM identity.app_user WHERE id = :dono")
                .param("dono", UUID.fromString(DONO))
                .query()
                .listOfRows();

        assertThat(donos).containsExactly(Map.of("id", DONO, "status", "ACTIVE"));
    }

    @Test
    void existemCasaEEmpresaComODonoComoOwner() {
        var espacos = jdbc.sql("SELECT id::text AS id, kind, name FROM identity.workspace ORDER BY id")
                .query()
                .listOfRows();
        var vinculos = jdbc.sql("SELECT workspace_id::text AS workspace_id, user_id::text AS user_id, role FROM identity.workspace_member ORDER BY workspace_id")
                .query()
                .listOfRows();

        assertThat(espacos).containsExactly(
                Map.of("id", CASA, "kind", "HOME", "name", "Casa"),
                Map.of("id", EMPRESA, "kind", "COMPANY", "name", "Empresa"));
        assertThat(vinculos).containsExactly(
                Map.of("workspace_id", CASA, "user_id", DONO, "role", "OWNER"),
                Map.of("workspace_id", EMPRESA, "user_id", DONO, "role", "OWNER"));
    }

    @Test
    void bancoRejeitaTipoDeEspacoForaDaLista() {
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO identity.workspace (id, kind, name) VALUES (gen_random_uuid(), 'OUTRO', 'x')").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaPapelForaDaLista() {
        assertThatThrownBy(() -> jdbc.sql("UPDATE identity.workspace_member SET role = 'ADMIN'").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailDoUsuarioNaoDiferenciaMaiusculas() {
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO identity.app_user (id, email, display_name, status)
                        VALUES (gen_random_uuid(), 'DONO@BOLSO.LOCAL', 'Outro', 'ACTIVE')
                        """).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void naoApagaUsuarioQueTemEspaco() {
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM identity.app_user").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
