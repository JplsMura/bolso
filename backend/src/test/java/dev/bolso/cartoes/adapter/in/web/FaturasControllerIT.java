package dev.bolso.cartoes.adapter.in.web;

import static dev.bolso.ApiDeTeste.CASA;
import static dev.bolso.ApiDeTeste.CONFORTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.bolso.ApiDeTeste;
import dev.bolso.TestcontainersConfig;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Faturas de ponta a ponta, num PostgreSQL real. As compras são de março de 2026 (fatura que já passou do
 * fechamento, então pode ser fechada e paga). Valores fictícios.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class FaturasControllerIT {

    @Autowired
    WebApplicationContext contexto;

    ApiDeTeste api;
    String cartao;

    @BeforeEach
    void monta() throws Exception {
        api = new ApiDeTeste(MockMvcBuilders.webAppContextSetup(contexto).build());
        cartao = api.criarCartao(CASA, "Cartão A", "4500.00");
    }

    String versao(int v) {
        return "{\"versao\":" + v + "}";
    }

    @Test
    void listaDoMesComItensTotalEDatas() throws Exception {
        var outro = api.criarCartao(CASA, "Cartão B", "1000.00");
        api.lancarNoCredito(CASA, "23.90", "Uber", "2026-03-10", cartao);
        api.lancarNoCredito(CASA, "39.90", "Streaming", "2026-03-12", cartao);
        api.lancarNoCredito(CASA, "50.00", "Depois do fechamento", "2026-03-25", cartao); // fatura de abril

        api.get(CASA, "/faturas?mes=2026-03")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-03"))
                .andExpect(jsonPath("$.faturas.length()").value(2));
        var a = api.faturaJson(CASA, "2026-03", cartao);
        assertThat((String) JsonPath.read(a, "$.fatura.status")).isEqualTo("OPEN");
        assertThat((String) JsonPath.read(a, "$.fatura.total")).isEqualTo("63.80");
        assertThat((String) JsonPath.read(a, "$.fatura.fechamento")).isEqualTo("2026-03-25");
        assertThat((String) JsonPath.read(a, "$.fatura.vencimento")).isEqualTo("2026-04-05");
        assertThat((Integer) JsonPath.read(a, "$.fatura.itens.length()")).isEqualTo(2);
        assertThat((String) JsonPath.read(a, "$.fatura.itens[0].descricao")).isEqualTo("Streaming");
        assertThat((String) JsonPath.read(a, "$.fatura.itens[0].metaId")).isEqualTo(CONFORTO);
        assertThat((String) JsonPath.read(api.faturaJson(CASA, "2026-03", outro), "$.cartao.nome")).isEqualTo("Cartão B");
        Map<String, Object> semCompra = JsonPath.read(api.faturaJson(CASA, "2026-03", outro), "$");
        assertThat(semCompra).doesNotContainKey("fatura"); // cartão sem compra no mês vem sem fatura
    }

    @Test
    void cicloCompletoFecharPagarDesfazerReabrir() throws Exception {
        api.lancarNoCredito(CASA, "23.90", "Uber", "2026-03-10", cartao);
        var id = api.faturaId(CASA, "2026-03", cartao);

        api.mudarFatura(CASA, id, "fechar", versao(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fatura.status").value("CLOSED"))
                .andExpect(jsonPath("$.fatura.total").value("23.90"));
        api.mudarFatura(CASA, id, "pagar", "{\"versao\":1,\"pagaEm\":\"2026-03-30\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fatura.status").value("PAID"))
                .andExpect(jsonPath("$.fatura.pagaEm").value("2026-03-30"))
                .andExpect(jsonPath("$.cartao.usado").value("0.00"));
        api.mudarFatura(CASA, id, "desfazer-pagamento", versao(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fatura.status").value("CLOSED"))
                .andExpect(jsonPath("$.fatura.pagaEm").doesNotExist());
        api.mudarFatura(CASA, id, "reabrir", versao(3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fatura.status").value("OPEN"))
                .andExpect(jsonPath("$.fatura.versao").value(4));
    }

    @Test
    void versaoVelhaDa409ETransicaoForaDoCicloDa422() throws Exception {
        api.lancarNoCredito(CASA, "23.90", "Uber", "2026-03-10", cartao);
        var id = api.faturaId(CASA, "2026-03", cartao);

        api.mudarFatura(CASA, id, "pagar", versao(0)).andExpect(status().is(422));
        api.mudarFatura(CASA, id, "reabrir", versao(0)).andExpect(status().is(422));
        api.mudarFatura(CASA, id, "fechar", versao(0)).andExpect(status().isOk());
        api.mudarFatura(CASA, id, "reabrir", versao(0)).andExpect(status().isConflict());
        api.mudarFatura(CASA, id, "pagar", "{\"versao\":1,\"pagaEm\":\"2026-03-20\"}").andExpect(status().is(422));
    }

    @Test
    void naoFechaAntesDaDataDeFechamento() throws Exception {
        var proximoMes = YearMonth.now(ZoneId.of("America/Sao_Paulo")).plusMonths(1);
        api.lancarNoCredito(CASA, "10.00", "x", proximoMes.atDay(1).toString(), cartao);
        var id = api.faturaId(CASA, proximoMes.toString(), cartao);

        api.mudarFatura(CASA, id, "fechar", versao(0))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("a partir de")));
    }

    @Test
    void faturaFechadaRecusaCompraMasAceitaMudarSoADescricao() throws Exception {
        var lancamento = api.lancarNoCredito(CASA, "23.90", "Uber", "2026-03-10", cartao);
        api.mudarFatura(CASA, api.faturaId(CASA, "2026-03", cartao), "fechar", versao(0)).andExpect(status().isOk());

        api.postar(CASA, "/lancamentos", ApiDeTeste.noCredito("5.00", "Outro", "2026-03-11", CONFORTO, cartao))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value("urn:bolso:problema:fatura-fechada"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("março/2026")));
        api.colocar(CASA, "/lancamentos/" + lancamento, ApiDeTeste.noCredito("30.00", "Uber", "2026-03-10", CONFORTO, cartao)
                        .replace("}", ",\"versao\":0}"))
                .andExpect(status().is(422));
        api.apagar(CASA, "/lancamentos/" + lancamento).andExpect(status().is(422));
        api.colocar(CASA, "/lancamentos/" + lancamento, ApiDeTeste.noCredito("23.90", "Uber até o centro", "2026-03-10", CONFORTO, cartao)
                        .replace("}", ",\"versao\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Uber até o centro"));
        assertThat((String) JsonPath.read(api.faturaJson(CASA, "2026-03", cartao), "$.fatura.total")).isEqualTo("23.90");
    }

    @Test
    void pagarAFaturaNaoMudaOsTotaisDoMes() throws Exception {
        api.lancarNoCredito(CASA, "23.90", "Uber", "2026-03-10", cartao);
        api.postar(CASA, "/lancamentos", ApiDeTeste.noPix("10.00", "Padaria", "2026-03-11", CONFORTO)).andExpect(status().isCreated());
        var antes = ApiDeTeste.corpo(api.get(CASA, "/lancamentos?mes=2026-03"));
        var id = api.faturaId(CASA, "2026-03", cartao);
        api.mudarFatura(CASA, id, "fechar", versao(0)).andExpect(status().isOk());
        api.mudarFatura(CASA, id, "pagar", "{\"versao\":1,\"pagaEm\":\"2026-03-30\"}").andExpect(status().isOk());

        api.get(CASA, "/lancamentos?mes=2026-03")
                .andExpect(jsonPath("$.totais.saidas").value("33.90"))
                .andExpect(jsonPath("$.itens.length()").value(2));
        assertThat((String) JsonPath.read(antes, "$.totais.saidas")).isEqualTo("33.90");
    }

    @Test
    void limiteUsadoIncluiParcelasFuturasEExcluiFaturasPagas() throws Exception {
        api.lancarNoCredito(CASA, "23.90", "Uber", "2026-03-10", cartao);
        api.postar(CASA, "/parcelamentos", ApiDeTeste.parcelamento("90.00", 3, null, "Curso", "2026-03-12", cartao))
                .andExpect(status().isCreated());
        api.get(CASA, "/cartoes").andExpect(jsonPath("$[0].usado").value("113.90"));

        var marco = api.faturaId(CASA, "2026-03", cartao);
        api.mudarFatura(CASA, marco, "fechar", versao(0)).andExpect(status().isOk());
        api.mudarFatura(CASA, marco, "pagar", "{\"versao\":1,\"pagaEm\":\"2026-03-30\"}").andExpect(status().isOk());

        api.get(CASA, "/cartoes")
                .andExpect(jsonPath("$[0].usado").value("60.00"))
                .andExpect(jsonPath("$[0].disponivel").value("4440.00"));
    }

    @Test
    void mesAusenteEhOAtualEMesMalFormadoDa400() throws Exception {
        api.get(CASA, "/faturas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(YearMonth.from(LocalDate.now(ZoneId.of("America/Sao_Paulo"))).toString()));
        api.get(CASA, "/faturas?mes=2026-13").andExpect(status().isBadRequest());
    }

    @Test
    void faturaDeOutroEspacoNaoExiste() throws Exception {
        api.lancarNoCredito(CASA, "23.90", "Uber", "2026-03-10", cartao);
        var id = api.faturaId(CASA, "2026-03", cartao);

        api.mudarFatura(ApiDeTeste.EMPRESA, id, "fechar", versao(0)).andExpect(status().isNotFound());
    }
}
