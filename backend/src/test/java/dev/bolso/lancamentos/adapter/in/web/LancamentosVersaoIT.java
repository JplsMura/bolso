package dev.bolso.lancamentos.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.bolso.TestcontainersConfig;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * A versão do lançamento com commits de verdade. Os outros ITs rodam dentro de uma transação que nunca
 * confirma; aqui cada requisição é uma transação própria, como em produção, e o teste limpa o que gravou.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class LancamentosVersaoIT {

    static final String CASA = "019a0000-0000-7000-8000-000000000011";
    static final String CONFORTO = "019a0000-0000-7000-8000-000000000102";

    @Autowired
    WebApplicationContext contexto;

    @Autowired
    JdbcClient jdbc;

    MockMvc mvc;
    UUID id;

    @BeforeEach
    void montaMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @AfterEach
    void limpa() {
        if (id != null) {
            jdbc.sql("DELETE FROM finance.transaction_tag WHERE transaction_id = :id").param("id", id).update();
            jdbc.sql("DELETE FROM finance.transaction WHERE id = :id").param("id", id).update();
        }
    }

    static String corpo(String descricao, long versao) {
        return """
                {"direcao":"OUT","valor":"10.00","descricao":"%s","data":"2026-03-10","metaId":"%s","formaPagamento":"PIX","versao":%d}
                """.formatted(descricao, CONFORTO, versao);
    }

    @Test
    void aVersaoSobeUmPorGravacaoEAUsadaNaProximaEdicao() throws Exception {
        var criado = mvc.perform(post("/api/v1/espacos/" + CASA + "/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Versão", 0).replace(",\"versao\":0", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versao").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        id = UUID.fromString(JsonPath.read(criado, "$.id"));
        var url = "/api/v1/espacos/" + CASA + "/lancamentos/" + id;

        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(corpo("Um", 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(1));
        // a versão devolvida é a que vale na próxima edição (sem 409 espúrio)
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(corpo("Dois", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(2));
        // e uma edição com versão antiga é recusada
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(corpo("Velha", 1)))
                .andExpect(status().isConflict());
    }
}
