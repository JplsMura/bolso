package dev.bolso.lancamentos.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class TagsControllerIT {

    static final String CASA = "019a0000-0000-7000-8000-000000000011";
    static final String EMPRESA = "019a0000-0000-7000-8000-000000000012";

    @Autowired
    WebApplicationContext contexto;

    MockMvc mvc;

    @BeforeEach
    void montaMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    String criar(String espaco, String corpo) throws Exception {
        return mvc.perform(post("/api/v1/espacos/" + espaco + "/tags").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void criaETagComTipoDeCusto() throws Exception {
        mvc.perform(post("/api/v1/espacos/" + CASA + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  Luz \",\"tipoCusto\":\"FIXED\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Luz"))
                .andExpect(jsonPath("$.tipoCusto").value("FIXED"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void tipoDeCustoNaoVemQuandoNaoTem() throws Exception {
        mvc.perform(post("/api/v1/espacos/" + CASA + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Uber\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoCusto").doesNotExist());
    }

    @Test
    void listaPorNomeSoDoEspaco() throws Exception {
        criar(CASA, "{\"nome\":\"Uber\"}");
        criar(CASA, "{\"nome\":\"Alimentação\"}");
        criar(EMPRESA, "{\"nome\":\"Nota\"}");

        mvc.perform(get("/api/v1/espacos/" + CASA + "/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Alimentação"))
                .andExpect(jsonPath("$[1].nome").value("Uber"));
    }

    @Test
    void nomeRepetidoComOutraCaixaDa422() throws Exception {
        criar(CASA, "{\"nome\":\"Luz\"}");

        mvc.perform(post("/api/v1/espacos/" + CASA + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"LUZ\"}"))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void mesmoNomeEmOutroEspacoPode() throws Exception {
        criar(CASA, "{\"nome\":\"Luz\"}");

        mvc.perform(post("/api/v1/espacos/" + EMPRESA + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Luz\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void nomeEmBrancoDa422EAusenteDa400() throws Exception {
        mvc.perform(post("/api/v1/espacos/" + CASA + "/tags").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"  \"}"))
                .andExpect(status().is(422));
        mvc.perform(post("/api/v1/espacos/" + CASA + "/tags").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void espacoInexistenteDa404() throws Exception {
        mvc.perform(get("/api/v1/espacos/" + UUID.randomUUID() + "/tags")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/espacos/" + UUID.randomUUID() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"x\"}"))
                .andExpect(status().isNotFound());
    }
}
