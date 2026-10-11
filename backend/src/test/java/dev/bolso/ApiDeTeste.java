package dev.bolso;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Atalhos dos testes de integração da 004 (cartões, faturas, parcelamentos). Valores sempre fictícios. */
public record ApiDeTeste(MockMvc mvc) {

    public static final String CASA = "019a0000-0000-7000-8000-000000000011";
    public static final String EMPRESA = "019a0000-0000-7000-8000-000000000012";
    public static final String CUSTOS_FIXOS = "019a0000-0000-7000-8000-000000000101";
    public static final String CONFORTO = "019a0000-0000-7000-8000-000000000102";

    static String base(String espaco) {
        return "/api/v1/espacos/" + espaco;
    }

    public ResultActions get(String espaco, String caminho) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(base(espaco) + caminho));
    }

    public ResultActions postar(String espaco, String caminho, String corpo) throws Exception {
        return mvc.perform(post(base(espaco) + caminho).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    public ResultActions colocar(String espaco, String caminho, String corpo) throws Exception {
        return mvc.perform(put(base(espaco) + caminho).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    public ResultActions apagar(String espaco, String caminho) throws Exception {
        return mvc.perform(delete(base(espaco) + caminho));
    }

    public static String cartao(String nome, int fecha, int vence, String limite) {
        return """
                {"nome":"%s","diaFechamento":%d,"diaVencimento":%d,"limite":"%s"}
                """.formatted(nome, fecha, vence, limite);
    }

    /** Cartão que fecha no dia 25 e vence no dia 5. Devolve o id. */
    public String criarCartao(String espaco, String nome, String limite) throws Exception {
        return id(postar(espaco, "/cartoes", cartao(nome, 25, 5, limite)).andExpect(status().isCreated()));
    }

    public static String noCredito(String valor, String descricao, String data, String meta, String cartao) {
        return """
                {"direcao":"OUT","valor":"%s","descricao":"%s","data":"%s","metaId":%s,"formaPagamento":"CREDIT","cartaoId":"%s","tagIds":[]}
                """.formatted(valor, descricao, data, meta == null ? "null" : "\"" + meta + "\"", cartao);
    }

    public static String noPix(String valor, String descricao, String data, String meta) {
        return """
                {"direcao":"OUT","valor":"%s","descricao":"%s","data":"%s","metaId":%s,"formaPagamento":"PIX","tagIds":[]}
                """.formatted(valor, descricao, data, meta == null ? "null" : "\"" + meta + "\"");
    }

    /** Gasto no cartão; devolve o id do lançamento. */
    public String lancarNoCredito(String espaco, String valor, String descricao, String data, String cartao) throws Exception {
        return id(postar(espaco, "/lancamentos", noCredito(valor, descricao, data, CONFORTO, cartao)).andExpect(status().isCreated()));
    }

    public static String parcelamento(String total, int parcelas, Integer atual, String descricao, String data, String cartao) {
        return """
                {"valorTotal":"%s","parcelas":%d,"parcelaAtual":%s,"descricao":"%s","data":"%s","cartaoId":"%s","metaId":"%s","tagIds":[]}
                """.formatted(total, parcelas, atual == null ? "null" : atual.toString(), descricao, data, cartao, CONFORTO);
    }

    /** A fatura do cartão no mês, como {@code GET /faturas} devolve (o primeiro item cujo cartão bate). */
    public String faturaJson(String espaco, String mes, String cartaoId) throws Exception {
        var json = corpo(get(espaco, "/faturas?mes=" + mes).andExpect(status().isOk()));
        java.util.List<Object> lista = JsonPath.read(json, "$.faturas[?(@.cartao.id == '" + cartaoId + "')]");
        return com.jayway.jsonpath.JsonPath.parse(lista.getFirst()).jsonString();
    }

    public String faturaId(String espaco, String mes, String cartaoId) throws Exception {
        return JsonPath.read(faturaJson(espaco, mes, cartaoId), "$.fatura.id");
    }

    public int versaoDaFatura(String espaco, String mes, String cartaoId) throws Exception {
        return JsonPath.read(faturaJson(espaco, mes, cartaoId), "$.fatura.versao");
    }

    public ResultActions mudarFatura(String espaco, String faturaId, String acao, String corpo) throws Exception {
        return postar(espaco, "/faturas/" + faturaId + "/" + acao, corpo);
    }

    public static String corpo(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString();
    }

    public static String id(ResultActions r) throws Exception {
        return JsonPath.read(corpo(r), "$.id");
    }
}
