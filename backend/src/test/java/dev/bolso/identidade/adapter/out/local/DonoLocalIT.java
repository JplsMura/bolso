package dev.bolso.identidade.adapter.out.local;

import static org.assertj.core.api.Assertions.assertThat;

import dev.bolso.TestcontainersConfig;
import dev.bolso.identidade.IdentidadeApi;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfig.class)
class DonoLocalIT {

    /** Id da migração V3; repetido de propósito, para o teste acusar se a constante do adaptador mudar. */
    static final UUID ID_DO_DONO = UUID.fromString("019a0000-0000-7000-8000-000000000001");

    @Autowired
    IdentidadeApi identidade;

    @Autowired
    JdbcClient jdbc;

    @Test
    void usuarioAtualEODonoLocal() {
        assertThat(identidade.usuarioAtualId()).isEqualTo(ID_DO_DONO);
    }

    @Test
    void oDonoLocalExisteNoBanco() {
        var existe = jdbc.sql("SELECT count(*) FROM identity.app_user WHERE id = :id")
                .param("id", ID_DO_DONO)
                .query(Long.class)
                .single();

        assertThat(existe).isEqualTo(1L);
    }
}
