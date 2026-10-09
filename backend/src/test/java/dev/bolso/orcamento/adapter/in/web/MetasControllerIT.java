package dev.bolso.orcamento.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.bolso.TestcontainersConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class MetasControllerIT {

    static final String CASA = "019a0000-0000-7000-8000-000000000011";
    static final String EMPRESA = "019a0000-0000-7000-8000-000000000012";

    @Autowired
    WebApplicationContext contexto;

    MockMvc mvc;

    @BeforeEach
    void montaMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void casaTemSeisMetasNaOrdemEComCores() throws Exception {
        mvc.perform(get("/api/v1/espacos/" + CASA + "/metas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].nome").value("Custos Fixos"))
                .andExpect(jsonPath("$[0].cor").value("#6EA8FE"))
                .andExpect(jsonPath("$[0].ordem").value(1))
                .andExpect(jsonPath("$[1].nome").value("Conforto"))
                .andExpect(jsonPath("$[2].nome").value("Metas"))
                .andExpect(jsonPath("$[3].nome").value("Prazeres"))
                .andExpect(jsonPath("$[4].nome").value("Liberdade Financeira"))
                .andExpect(jsonPath("$[5].nome").value("Conhecimento"))
                .andExpect(jsonPath("$[5].cor").value("#5CD6E8"));
    }

    @Test
    void empresaNaoTemMetas() throws Exception {
        mvc.perform(get("/api/v1/espacos/" + EMPRESA + "/metas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void espacoInexistenteDa404() throws Exception {
        mvc.perform(get("/api/v1/espacos/" + UUID.randomUUID() + "/metas")).andExpect(status().isNotFound());
    }
}
