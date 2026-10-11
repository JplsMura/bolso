package dev.bolso.lancamentos.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.bolso.TestcontainersConfig;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** A API de lançamentos de ponta a ponta, num PostgreSQL real. Cada teste desfaz o que inseriu. */
@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class LancamentosControllerIT {

    static final String CASA = "019a0000-0000-7000-8000-000000000011";
    static final String EMPRESA = "019a0000-0000-7000-8000-000000000012";
    static final String CUSTOS_FIXOS = "019a0000-0000-7000-8000-000000000101";
    static final String CONFORTO = "019a0000-0000-7000-8000-000000000102";

    @Autowired
    WebApplicationContext contexto;

    @Autowired
    JdbcClient jdbc;

    MockMvc mvc;

    @BeforeEach
    void montaMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    // ---------- ajudantes ----------

    static String saida(String valor, String descricao, String data, String meta, String pagamento, String... tagIds) {
        var tags = tagIds.length == 0 ? "[]" : "[\"" + String.join("\",\"", tagIds) + "\"]";
        return """
                {"direcao":"OUT","valor":"%s","descricao":"%s","data":"%s","metaId":%s,"formaPagamento":"%s","tagIds":%s}
                """.formatted(valor, descricao, data, meta == null ? "null" : "\"" + meta + "\"", pagamento, tags);
    }

    static String entrada(String valor, String descricao, String data) {
        return """
                {"direcao":"IN","valor":"%s","descricao":"%s","data":"%s"}
                """.formatted(valor, descricao, data);
    }

    ResultActions postar(String espaco, String corpo) throws Exception {
        return mvc.perform(post("/api/v1/espacos/" + espaco + "/lancamentos").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    String criar(String espaco, String corpo) throws Exception {
        var resposta = postar(espaco, corpo).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    String criarTag(String espaco, String nome) throws Exception {
        var resposta = mvc.perform(post("/api/v1/espacos/" + espaco + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    ResultActions listar(String espaco, String consulta) throws Exception {
        return mvc.perform(get("/api/v1/espacos/" + espaco + "/lancamentos" + consulta));
    }

    // ---------- criar, ler, editar, excluir ----------

    @Test
    void criaComLocationEDevolveOLancamento() throws Exception {
        var tag = criarTag(CASA, "Luz");

        postar(CASA, saida("110.00", "Conta de luz", "2026-03-10", CUSTOS_FIXOS, "PIX", tag))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/v1/espacos/" + CASA + "/lancamentos/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.direcao").value("OUT"))
                .andExpect(jsonPath("$.valor").value("110.00"))
                .andExpect(jsonPath("$.descricao").value("Conta de luz"))
                .andExpect(jsonPath("$.data").value("2026-03-10"))
                .andExpect(jsonPath("$.mesReferencia").value("2026-03"))
                .andExpect(jsonPath("$.metaId").value(CUSTOS_FIXOS))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.tags[0].nome").value("Luz"))
                .andExpect(jsonPath("$.versao").value(0));
    }

    @Test
    void entradaNaoTemMetaNemFormaDePagamentoNoJson() throws Exception {
        postar(CASA, entrada("5000.00", "Salário", "2026-03-05"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direcao").value("IN"))
                .andExpect(jsonPath("$.metaId").doesNotExist())
                .andExpect(jsonPath("$.formaPagamento").doesNotExist())
                .andExpect(jsonPath("$.tags.length()").value(0));
    }

    @Test
    void leUmLancamento() throws Exception {
        var id = criar(CASA, saida("89.90", "Mercado", "2026-03-11", CONFORTO, "DEBIT"));

        mvc.perform(get("/api/v1/espacos/" + CASA + "/lancamentos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.valor").value("89.90"));
    }

    @Test
    void editaComAVersaoCertaESobeAVersao() throws Exception {
        var id = criar(CASA, saida("110.00", "Luz", "2026-03-10", CUSTOS_FIXOS, "PIX"));
        var tag = criarTag(CASA, "Energia");

        mvc.perform(put("/api/v1/espacos/" + CASA + "/lancamentos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direcao":"OUT","valor":"120.50","descricao":"Luz de março","data":"2026-04-02",
                                 "metaId":"%s","formaPagamento":"DEBIT","tagIds":["%s"],"versao":0}
                                """.formatted(CONFORTO, tag)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("120.50"))
                .andExpect(jsonPath("$.descricao").value("Luz de março"))
                .andExpect(jsonPath("$.mesReferencia").value("2026-04"))
                .andExpect(jsonPath("$.metaId").value(CONFORTO))
                .andExpect(jsonPath("$.tags[0].nome").value("Energia"))
                .andExpect(jsonPath("$.versao").value(1));
    }

    @Test
    void editarComVersaoErradaDa409() throws Exception {
        var id = criar(CASA, saida("110.00", "Luz", "2026-03-10", CUSTOS_FIXOS, "PIX"));
        var corpo = """
                {"direcao":"OUT","valor":"1.00","descricao":"x","data":"2026-03-10","metaId":"%s","formaPagamento":"PIX","versao":%d}
                """;
        var url = "/api/v1/espacos/" + CASA + "/lancamentos/" + id;

        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(corpo.formatted(CUSTOS_FIXOS, 0)))
                .andExpect(status().isOk());
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(corpo.formatted(CUSTOS_FIXOS, 0)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void editarSemVersaoDa400() throws Exception {
        var id = criar(CASA, saida("110.00", "Luz", "2026-03-10", CUSTOS_FIXOS, "PIX"));

        mvc.perform(put("/api/v1/espacos/" + CASA + "/lancamentos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saida("1.00", "x", "2026-03-10", CUSTOS_FIXOS, "PIX")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void excluiEDepoisDa404ESomeDaLista() throws Exception {
        var id = criar(CASA, saida("110.00", "Luz", "2026-03-10", CUSTOS_FIXOS, "PIX"));

        mvc.perform(delete("/api/v1/espacos/" + CASA + "/lancamentos/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/espacos/" + CASA + "/lancamentos/" + id)).andExpect(status().isNotFound());
        listar(CASA, "?mes=2026-03").andExpect(jsonPath("$.itens.length()").value(0));
        mvc.perform(delete("/api/v1/espacos/" + CASA + "/lancamentos/" + id)).andExpect(status().isNotFound());
        // exclusão lógica: a linha continua no banco
        var guardadas = jdbc.sql("SELECT count(*) FROM finance.transaction WHERE id = :id AND deleted_at IS NOT NULL")
                .param("id", UUID.fromString(id))
                .query(Long.class)
                .single();
        assertThat(guardadas).isOne();
    }

    @Test
    void lancamentoNaoApareceNemSeEditaPorOutroEspaco() throws Exception {
        var id = criar(CASA, saida("110.00", "Luz", "2026-03-10", CUSTOS_FIXOS, "PIX"));

        mvc.perform(get("/api/v1/espacos/" + EMPRESA + "/lancamentos/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/espacos/" + EMPRESA + "/lancamentos/" + id)).andExpect(status().isNotFound());
    }

    // ---------- lista do mês e totais ----------

    @Test
    void listaDoMesTemTotaisCertosEIgnoraOutroMesEExcluidos() throws Exception {
        criar(CASA, entrada("5000.00", "Salário", "2026-03-05"));
        criar(CASA, saida("110.00", "Luz", "2026-03-10", CUSTOS_FIXOS, "PIX"));
        criar(CASA, saida("89.90", "Mercado", "2026-03-11", CONFORTO, "DEBIT"));
        criar(CASA, saida("999.00", "Abril", "2026-04-01", CONFORTO, "DEBIT"));
        var excluido = criar(CASA, saida("500.00", "Excluído", "2026-03-12", CONFORTO, "DEBIT"));
        mvc.perform(delete("/api/v1/espacos/" + CASA + "/lancamentos/" + excluido)).andExpect(status().isNoContent());

        listar(CASA, "?mes=2026-03")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-03"))
                .andExpect(jsonPath("$.totais.entradas").value("5000.00"))
                .andExpect(jsonPath("$.totais.saidas").value("199.90"))
                .andExpect(jsonPath("$.totais.sobra").value("4800.10"))
                .andExpect(jsonPath("$.itens.length()").value(3))
                // do mais recente para o mais antigo
                .andExpect(jsonPath("$.itens[0].descricao").value("Mercado"))
                .andExpect(jsonPath("$.itens[2].descricao").value("Salário"));
    }

    @Test
    void sobraPodeSerNegativa() throws Exception {
        criar(CASA, saida("30.00", "Gasto", "2026-03-10", CONFORTO, "PIX"));

        listar(CASA, "?mes=2026-03").andExpect(jsonPath("$.totais.sobra").value("-30.00"));
    }

    @Test
    void mesSemLancamentosTemListaVaziaETotaisZero() throws Exception {
        listar(CASA, "?mes=2020-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.totais.entradas").value("0.00"))
                .andExpect(jsonPath("$.totais.saidas").value("0.00"))
                .andExpect(jsonPath("$.totais.sobra").value("0.00"));
    }

    @Test
    void semMesUsaOMesAtualDeSaoPaulo() throws Exception {
        var hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        criar(CASA, saida("1.00", "De hoje", hoje.toString(), CONFORTO, "PIX"));

        listar(CASA, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(YearMonth.from(hoje).toString()))
                .andExpect(jsonPath("$.itens[?(@.descricao == 'De hoje')]").isNotEmpty());
    }

    @Test
    void mesMalFormadoDa400() throws Exception {
        listar(CASA, "?mes=2026-13").andExpect(status().isBadRequest());
        listar(CASA, "?mes=marco").andExpect(status().isBadRequest());
    }

    // ---------- filtros e busca ----------

    @Test
    void filtraPorMetaTagPagamentoEDirecao() throws Exception {
        var luz = criarTag(CASA, "Luz");
        criar(CASA, saida("110.00", "Conta A", "2026-03-10", CUSTOS_FIXOS, "PIX", luz));
        criar(CASA, saida("20.00", "Conta B", "2026-03-11", CONFORTO, "DEBIT"));
        criar(CASA, entrada("300.00", "Conta C", "2026-03-12"));

        listar(CASA, "?mes=2026-03&metaId=" + CONFORTO)
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("Conta B"));
        listar(CASA, "?mes=2026-03&tagId=" + luz)
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("Conta A"));
        listar(CASA, "?mes=2026-03&pagamento=DEBIT")
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("Conta B"));
        listar(CASA, "?mes=2026-03&direcao=IN")
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("Conta C"));
    }

    @Test
    void totaisSaoDoMesInteiroMesmoComFiltro() throws Exception {
        criar(CASA, saida("10.00", "Um", "2026-03-10", CUSTOS_FIXOS, "PIX"));
        criar(CASA, saida("20.00", "Dois", "2026-03-11", CONFORTO, "PIX"));

        listar(CASA, "?mes=2026-03&metaId=" + CONFORTO)
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.totais.saidas").value("30.00"));
    }

    @Test
    void buscaPorDescricaoEPorTagSemDiferenciarCaixa() throws Exception {
        var uber = criarTag(CASA, "Uber");
        criar(CASA, saida("25.00", "Corrida ao aeroporto", "2026-03-10", CONFORTO, "PIX", uber));
        criar(CASA, saida("40.00", "Padaria", "2026-03-11", CONFORTO, "PIX"));

        listar(CASA, "?mes=2026-03&q=AEROPORTO")
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("Corrida ao aeroporto"));
        listar(CASA, "?mes=2026-03&q=uber")
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("Corrida ao aeroporto"));
        listar(CASA, "?mes=2026-03&q=nada")
                .andExpect(jsonPath("$.itens.length()").value(0));
    }

    @Test
    void porcentoEUnderscoreNaBuscaSaoTextoNaoCuringa() throws Exception {
        criar(CASA, saida("1.00", "Desconto 10% na loja", "2026-03-10", CONFORTO, "PIX"));
        criar(CASA, saida("1.00", "Desconto 10 reais", "2026-03-10", CONFORTO, "PIX"));
        criar(CASA, saida("1.00", "arquivo_final", "2026-03-10", CONFORTO, "PIX"));
        criar(CASA, saida("1.00", "arquivoXfinal", "2026-03-10", CONFORTO, "PIX"));

        // .param() não codifica de novo: o texto digitado chega ao servidor como "%"
        mvc.perform(get("/api/v1/espacos/" + CASA + "/lancamentos").param("mes", "2026-03").param("q", "%"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("Desconto 10% na loja"));
        mvc.perform(get("/api/v1/espacos/" + CASA + "/lancamentos").param("mes", "2026-03").param("q", "_"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("arquivo_final"));
    }

    @Test
    void buscaEmBrancoNaoFiltra() throws Exception {
        criar(CASA, saida("1.00", "Um", "2026-03-10", CONFORTO, "PIX"));

        mvc.perform(get("/api/v1/espacos/" + CASA + "/lancamentos").param("mes", "2026-03").param("q", "  "))
                .andExpect(jsonPath("$.itens.length()").value(1));
    }

    // ---------- erros ----------

    @Test
    void valorMalFormadoDa400() throws Exception {
        for (var valor : new String[] {"110", "110.5", "-1.00", "abc"}) {
            postar(CASA, saida(valor, "x", "2026-03-10", CONFORTO, "PIX"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Test
    void camposObrigatoriosEEnumsInvalidosDao400() throws Exception {
        postar(CASA, "{}").andExpect(status().isBadRequest());
        postar(CASA, "{\"direcao\":\"OUT\",\"valor\":\"1.00\",\"descricao\":\"x\"}").andExpect(status().isBadRequest());
        postar(CASA, saida("1.00", "x", "2026-03-10", CONFORTO, "FIADO")).andExpect(status().isBadRequest());
        postar(CASA, saida("1.00", "x", "10/03/2026", CONFORTO, "PIX")).andExpect(status().isBadRequest());
    }

    @Test
    void regrasDeNegocioDao422() throws Exception {
        var outraMeta = UUID.randomUUID().toString();
        postar(CASA, saida("0.00", "zero", "2026-03-10", CONFORTO, "PIX")).andExpect(status().is(422));
        postar(CASA, saida("1.00", "   ", "2026-03-10", CONFORTO, "PIX")).andExpect(status().is(422));
        postar(CASA, saida("1.00", "sem meta", "2026-03-10", null, "PIX")).andExpect(status().is(422));
        postar(CASA, saida("1.00", "meta que não existe", "2026-03-10", outraMeta, "PIX")).andExpect(status().is(422));
        postar(CASA, saida("1.00", "cartão", "2026-03-10", CONFORTO, "CREDIT"))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").isNotEmpty());
        postar(CASA, saida("1.00", "tag que não existe", "2026-03-10", CONFORTO, "PIX", UUID.randomUUID().toString()))
                .andExpect(status().is(422));
        postar(CASA, """
                {"direcao":"IN","valor":"1.00","descricao":"x","data":"2026-03-10","metaId":"%s"}
                """.formatted(CONFORTO)).andExpect(status().is(422));
    }

    @Test
    void metaETagDeOutroEspacoSaoRecusadas() throws Exception {
        var tagDaEmpresa = criarTag(EMPRESA, "Nota");

        // meta da Casa na Empresa: a Empresa não tem metas
        postar(EMPRESA, saida("1.00", "x", "2026-03-10", CONFORTO, "PIX")).andExpect(status().is(422));
        // tag da Empresa na Casa
        postar(CASA, saida("1.00", "x", "2026-03-10", CONFORTO, "PIX", tagDaEmpresa)).andExpect(status().is(422));
    }

    @Test
    void empresaLancaSaidaSemMeta() throws Exception {
        postar(EMPRESA, saida("300.00", "Nota fiscal", "2026-03-10", null, "BOLETO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metaId").doesNotExist());
    }

    @Test
    void espacoInexistenteOuDeOutroUsuarioDa404() throws Exception {
        var outro = criaEspacoDeOutroUsuario();
        for (var espaco : new String[] {UUID.randomUUID().toString(), outro.toString()}) {
            listar(espaco, "").andExpect(status().isNotFound());
            postar(espaco, entrada("1.00", "x", "2026-03-10")).andExpect(status().isNotFound());
        }
    }

    @Test
    void visualizadorLeMasNaoGrava() throws Exception {
        var id = criar(CASA, saida("1.00", "x", "2026-03-10", CONFORTO, "PIX"));
        jdbc.sql("UPDATE identity.workspace_member SET role = 'VIEWER' WHERE workspace_id = :w")
                .param("w", UUID.fromString(CASA))
                .update();

        listar(CASA, "?mes=2026-03").andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
        postar(CASA, entrada("1.00", "y", "2026-03-10"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(put("/api/v1/espacos/" + CASA + "/lancamentos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entrada("1.00", "y", "2026-03-10").replace("}", ",\"versao\":0}")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/espacos/" + CASA + "/lancamentos/" + id)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/espacos/" + CASA + "/tags").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void editarQueNaoExisteDa404() throws Exception {
        mvc.perform(put("/api/v1/espacos/" + CASA + "/lancamentos/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saida("1.00", "x", "2026-03-10", CONFORTO, "PIX").replace("}", ",\"versao\":0}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void editarNaoMexeEmLancamentoDeOutroEspaco() throws Exception {
        var id = criar(CASA, saida("1.00", "x", "2026-03-10", CONFORTO, "PIX"));

        mvc.perform(put("/api/v1/espacos/" + EMPRESA + "/lancamentos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saida("1.00", "x", "2026-03-10", null, "PIX").replace("}", ",\"versao\":0}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void editarComMetaQueNaoExisteDa422() throws Exception {
        var id = criar(CASA, saida("1.00", "x", "2026-03-10", CONFORTO, "PIX"));

        mvc.perform(put("/api/v1/espacos/" + CASA + "/lancamentos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saida("1.00", "x", "2026-03-10", UUID.randomUUID().toString(), "PIX")
                                .replace("}", ",\"versao\":0}")))
                .andExpect(status().is(422));
    }

    @Test
    void maisDeDezTagsDa422() throws Exception {
        var ids = new String[11];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = criarTag(CASA, "tag" + i);
        }

        postar(CASA, saida("1.00", "x", "2026-03-10", CONFORTO, "PIX", ids)).andExpect(status().is(422));
    }

    @Test
    void filtrarPorCreditoNaoQuebraEVoltaVazio() throws Exception {
        criar(CASA, saida("1.00", "x", "2026-03-10", CONFORTO, "PIX"));

        listar(CASA, "?mes=2026-03&pagamento=CREDIT").andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(0));
    }

    @Test
    void idQueNaoEUuidDa400() throws Exception {
        mvc.perform(get("/api/v1/espacos/" + CASA + "/lancamentos/abc")).andExpect(status().isBadRequest());
    }

    // ---------- contrato ----------

    @Test
    void openApiTrazOsEndpointsComValorEmTexto() throws Exception {
        var json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat((Object) JsonPath.read(json, "$.paths['/api/v1/espacos/{espacoId}/lancamentos']")).isNotNull();
        assertThat((Object) JsonPath.read(json, "$.paths['/api/v1/espacos/{espacoId}/lancamentos/{id}']")).isNotNull();
        assertThat((Object) JsonPath.read(json, "$.paths['/api/v1/espacos/{espacoId}/tags']")).isNotNull();
        assertThat((Object) JsonPath.read(json, "$.paths['/api/v1/espacos/{espacoId}/metas']")).isNotNull();
        assertThat((String) JsonPath.read(json, "$.components.schemas.LancamentoResponse.properties.valor.type")).isEqualTo("string");
        assertThat((String) JsonPath.read(json, "$.components.schemas.TotaisResponse.properties.sobra.type")).isEqualTo("string");
        assertThat((java.util.List<String>) JsonPath.read(json, "$.components.schemas.LancamentoResponse.required"))
                .contains("id", "direcao", "valor", "descricao", "data", "mesReferencia", "tags", "versao")
                .doesNotContain("metaId", "formaPagamento");
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
