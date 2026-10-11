package dev.bolso.lancamentos.adapter.in.web;

import static dev.bolso.ApiDeTeste.CASA;
import static dev.bolso.ApiDeTeste.CONFORTO;
import static dev.bolso.ApiDeTeste.parcelamento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.bolso.ApiDeTeste;
import dev.bolso.TestcontainersConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Compra parcelada de ponta a ponta, num PostgreSQL real. Compras de março de 2026; valores fictícios. */
@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class ParcelamentosControllerIT {

    @Autowired
    WebApplicationContext contexto;

    ApiDeTeste api;
    String cartao;

    @BeforeEach
    void monta() throws Exception {
        api = new ApiDeTeste(MockMvcBuilders.webAppContextSetup(contexto).build());
        cartao = api.criarCartao(CASA, "Cartão A", "4500.00");
    }

    String criar(int parcelas) throws Exception {
        return ApiDeTeste.id(api.postar(CASA, "/parcelamentos", parcelamento("100.00", parcelas, null, "Curso online", "2026-03-12", cartao))
                .andExpect(status().isCreated()));
    }

    @Test
    void criaAsParcelasUmaPorFaturaEMesDoOrcamento() throws Exception {
        api.postar(CASA, "/parcelamentos", parcelamento("100.00", 3, null, "Curso online", "2026-03-12", cartao))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/parcelamentos/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.valorTotal").value("100.00"))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.parcelaAtual").value(1))
                .andExpect(jsonPath("$.metaId").value(CONFORTO))
                .andExpect(jsonPath("$.itens[*].valor").value(org.hamcrest.Matchers.contains("33.34", "33.33", "33.33")))
                .andExpect(jsonPath("$.itens[*].faturaMes").value(org.hamcrest.Matchers.contains("2026-03", "2026-04", "2026-05")))
                .andExpect(jsonPath("$.itens[*].faturaAberta").value(org.hamcrest.Matchers.contains(true, true, true)));

        api.get(CASA, "/lancamentos?mes=2026-04")
                .andExpect(jsonPath("$.itens[0].descricao").value("Curso online"))
                .andExpect(jsonPath("$.itens[0].data").value("2026-04-12"))
                .andExpect(jsonPath("$.itens[0].parcela.numero").value(2))
                .andExpect(jsonPath("$.itens[0].parcela.total").value(3))
                .andExpect(jsonPath("$.itens[0].cartaoId").value(cartao))
                .andExpect(jsonPath("$.itens[0].faturaMes").value("2026-04"))
                .andExpect(jsonPath("$.totais.saidas").value("33.33"));
        var abril = api.faturaJson(CASA, "2026-04", cartao);
        assertThat((Integer) JsonPath.read(abril, "$.fatura.itens[0].parcela.numero")).isEqualTo(2);
    }

    @Test
    void emAndamentoCriaSoAsQueFaltam() throws Exception {
        api.postar(CASA, "/parcelamentos", parcelamento("150.00", 6, 4, "Curso", "2025-12-12", cartao))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens.length()").value(3))
                .andExpect(jsonPath("$.itens[*].numero").value(org.hamcrest.Matchers.contains(4, 5, 6)))
                .andExpect(jsonPath("$.itens[*].faturaMes").value(org.hamcrest.Matchers.contains("2026-03", "2026-04", "2026-05")));
    }

    @Test
    void leOParcelamento() throws Exception {
        var id = criar(3);

        api.get(CASA, "/parcelamentos/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Curso online"))
                .andExpect(jsonPath("$.versao").value(0))
                .andExpect(jsonPath("$.itens.length()").value(3));
        api.get(CASA, "/parcelamentos/" + UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void editarMudaSoAsParcelasEmFaturaAberta() throws Exception {
        var id = criar(3);
        api.mudarFatura(CASA, api.faturaId(CASA, "2026-03", cartao), "fechar", "{\"versao\":0}").andExpect(status().isOk());

        api.colocar(CASA, "/parcelamentos/" + id, """
                        {"descricao":"Curso de Java","metaId":"%s","tagIds":[],"versao":0}
                        """.formatted(ApiDeTeste.CUSTOS_FIXOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Curso de Java"))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.metaId").value(ApiDeTeste.CUSTOS_FIXOS))
                .andExpect(jsonPath("$.itens[*].faturaAberta").value(org.hamcrest.Matchers.contains(false, true, true)));

        api.get(CASA, "/lancamentos?mes=2026-03").andExpect(jsonPath("$.itens[0].descricao").value("Curso online"));
        api.get(CASA, "/lancamentos?mes=2026-04").andExpect(jsonPath("$.itens[0].descricao").value("Curso de Java"));
        api.colocar(CASA, "/parcelamentos/" + id, """
                        {"descricao":"Outro","metaId":"%s","tagIds":[],"versao":0}
                        """.formatted(CONFORTO))
                .andExpect(status().isConflict());
    }

    @Test
    void excluirTiraSoAsParcelasEmFaturaAberta() throws Exception {
        var id = criar(3);
        api.mudarFatura(CASA, api.faturaId(CASA, "2026-03", cartao), "fechar", "{\"versao\":0}").andExpect(status().isOk());

        api.apagar(CASA, "/parcelamentos/" + id).andExpect(status().isNoContent());

        api.get(CASA, "/parcelamentos/" + id).andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
        api.get(CASA, "/lancamentos?mes=2026-04").andExpect(jsonPath("$.itens.length()").value(0));
        api.apagar(CASA, "/parcelamentos/" + id).andExpect(status().is(422));
    }

    @Test
    void excluirTudoApagaOParcelamento() throws Exception {
        var id = criar(2);

        api.apagar(CASA, "/parcelamentos/" + id).andExpect(status().isNoContent());

        api.get(CASA, "/parcelamentos/" + id).andExpect(status().isNotFound());
        api.get(CASA, "/cartoes").andExpect(jsonPath("$[0].usado").value("0.00"));
    }

    @Test
    void parcelaNaoSeEditaNemSeExcluiPorLancamentos() throws Exception {
        criar(2);
        var parcela = (String) JsonPath.read(ApiDeTeste.corpo(api.get(CASA, "/lancamentos?mes=2026-03")), "$.itens[0].id");

        api.colocar(CASA, "/lancamentos/" + parcela, ApiDeTeste.noCredito("50.00", "x", "2026-03-12", CONFORTO, cartao)
                        .replace("}", ",\"versao\":0}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("parcelamento")));
        api.apagar(CASA, "/lancamentos/" + parcela).andExpect(status().is(422));
    }

    @Test
    void regrasDa422EFormatoDa400() throws Exception {
        api.postar(CASA, "/parcelamentos", parcelamento("100", 3, null, "x", "2026-03-12", cartao)).andExpect(status().isBadRequest());
        api.postar(CASA, "/parcelamentos", parcelamento("100.00", 1, null, "x", "2026-03-12", cartao)).andExpect(status().is(422));
        api.postar(CASA, "/parcelamentos", parcelamento("100.00", 49, null, "x", "2026-03-12", cartao)).andExpect(status().is(422));
        api.postar(CASA, "/parcelamentos", parcelamento("0.05", 6, null, "x", "2026-03-12", cartao)).andExpect(status().is(422));
        api.postar(CASA, "/parcelamentos", parcelamento("100.00", 3, 4, "x", "2026-03-12", cartao)).andExpect(status().is(422));
        api.postar(CASA, "/parcelamentos", parcelamento("100.00", 3, null, "x", "2026-03-12", UUID.randomUUID().toString()))
                .andExpect(status().is(422));
    }

    @Test
    void parcelaEmFaturaFechadaRecusaOParcelamentoInteiro() throws Exception {
        api.lancarNoCredito(CASA, "10.00", "x", "2026-04-10", cartao);
        api.mudarFatura(CASA, api.faturaId(CASA, "2026-04", cartao), "fechar", "{\"versao\":0}").andExpect(status().isOk());

        api.postar(CASA, "/parcelamentos", parcelamento("100.00", 3, null, "x", "2026-03-12", cartao))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value("urn:bolso:problema:fatura-fechada"));
        // as parcelas já gravadas somem no rollback da requisição; aqui o teste inteiro é uma transação só, então
        // não dá para conferir pela lista

    }
}
