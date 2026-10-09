package dev.bolso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Sobe a aplicação num PostgreSQL vazio: o Flyway aplica todas as migrações do zero. */
@SpringBootTest
@Import(TestcontainersConfig.class)
class MigracoesIT {

    @Autowired
    JdbcClient jdbc;

    @Test
    void criaOsCincoSchemas() {
        var schemas = jdbc.sql("SELECT schema_name FROM information_schema.schemata")
                .query(String.class)
                .list();

        assertThat(schemas).contains("identity", "finance", "billing", "planning", "platform");
    }

    @Test
    void criaAsExtensoes() {
        var extensoes = jdbc.sql("SELECT extname FROM pg_extension").query(String.class).list();

        assertThat(extensoes).contains("citext", "btree_gist");
    }
}
