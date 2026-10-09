package dev.bolso.identidade.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.bolso.TestcontainersConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** A API de espaços de ponta a ponta, num PostgreSQL real. Cada teste desfaz o que inseriu. */
@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class EspacosControllerIT {

    static final String CASA = "019a0000-0000-7000-8000-000000000011";

    @Autowired
    WebApplicationContext contexto;

    @Autowired
    JdbcClient jdbc;

    MockMvc mvc;

    @BeforeEach
    void montaMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void listaCasaEEmpresaDoDono() throws Exception {
        mvc.perform(get("/api/v1/espacos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(CASA))
                .andExpect(jsonPath("$[0].tipo").value("HOME"))
                .andExpect(jsonPath("$[0].nome").value("Casa"))
                .andExpect(jsonPath("$[0].papel").value("OWNER"))
                .andExpect(jsonPath("$[1].tipo").value("COMPANY"))
                .andExpect(jsonPath("$[1].nome").value("Empresa"))
                .andExpect(jsonPath("$[1].papel").value("OWNER"));
    }

    @Test
    void listaNaoMostraEspacoDeOutroUsuario() throws Exception {
        criaEspacoDeOutroUsuario();

        mvc.perform(get("/api/v1/espacos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscaACasa() throws Exception {
        mvc.perform(get("/api/v1/espacos/" + CASA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CASA))
                .andExpect(jsonPath("$.tipo").value("HOME"));
    }

    @Test
    void idQueNaoExisteDa404ProblemJson() throws Exception {
        mvc.perform(get("/api/v1/espacos/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void espacoDeOutroUsuarioDa404IgualAoQueNaoExiste() throws Exception {
        var deOutro = criaEspacoDeOutroUsuario();

        mvc.perform(get("/api/v1/espacos/" + deOutro))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void idQueNaoEUuidDa400() throws Exception {
        mvc.perform(get("/api/v1/espacos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private UUID criaEspacoDeOutroUsuario() {
        var usuario = UUID.randomUUID();
        var espaco = UUID.randomUUID();
        jdbc.sql("INSERT INTO identity.app_user (id, email, display_name, status) VALUES (:id, 'outro@teste.local', 'Outro', 'ACTIVE')")
                .param("id", usuario)
                .update();
        jdbc.sql("INSERT INTO identity.workspace (id, kind, name) VALUES (:id, 'HOME', 'Casa de outro')")
                .param("id", espaco)
                .update();
        jdbc.sql("INSERT INTO identity.workspace_member (workspace_id, user_id, role) VALUES (:w, :u, 'OWNER')")
                .param("w", espaco)
                .param("u", usuario)
                .update();
        return espaco;
    }
}
