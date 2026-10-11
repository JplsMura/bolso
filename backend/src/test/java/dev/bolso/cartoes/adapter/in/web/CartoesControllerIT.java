package dev.bolso.cartoes.adapter.in.web;

import static dev.bolso.ApiDeTeste.CASA;
import static dev.bolso.ApiDeTeste.cartao;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.bolso.ApiDeTeste;
import dev.bolso.TestcontainersConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Cadastro de cartões de ponta a ponta, num PostgreSQL real. Cada teste desfaz o que inseriu. Valores fictícios. */
@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class CartoesControllerIT {

    @Autowired
    WebApplicationContext contexto;

    @Autowired
    JdbcClient jdbc;

    ApiDeTeste api;

    @BeforeEach
    void monta() {
        api = new ApiDeTeste(MockMvcBuilders.webAppContextSetup(contexto).build());
    }

    @Test
    void criaComLocationEListaComLimiteLivre() throws Exception {
        api.postar(CASA, "/cartoes", cartao("Cartão A", 25, 5, "4500.00"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/cartoes/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.nome").value("Cartão A"))
                .andExpect(jsonPath("$.diaFechamento").value(25))
                .andExpect(jsonPath("$.diaVencimento").value(5))
                .andExpect(jsonPath("$.limite").value("4500.00"))
                .andExpect(jsonPath("$.usado").value("0.00"))
                .andExpect(jsonPath("$.disponivel").value("4500.00"))
                .andExpect(jsonPath("$.versao").value(0));

        api.get(CASA, "/cartoes").andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Cartão A"));
    }

    @Test
    void usadoEDisponivelSomamOsGastos() throws Exception {
        var id = api.criarCartao(CASA, "Cartão A", "4500.00");
        api.lancarNoCredito(CASA, "23.90", "Uber", "2026-03-10", id);
        api.lancarNoCredito(CASA, "100.00", "Mercado", "2026-03-28", id); // fatura de abril

        api.get(CASA, "/cartoes")
                .andExpect(jsonPath("$[0].usado").value("123.90"))
                .andExpect(jsonPath("$[0].disponivel").value("4376.10"));
    }

    @Test
    void editaComAVersaoCertaEDa409ComAVelha() throws Exception {
        var id = api.criarCartao(CASA, "Cartão A", "4500.00");

        api.colocar(CASA, "/cartoes/" + id, """
                        {"nome":"Cartão A2","diaFechamento":10,"diaVencimento":20,"limite":"5000.00","versao":0}
                        """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cartão A2"))
                .andExpect(jsonPath("$.versao").value(1));
        api.colocar(CASA, "/cartoes/" + id, """
                        {"nome":"Cartão A3","diaFechamento":10,"diaVencimento":20,"limite":"5000.00","versao":0}
                        """)
                .andExpect(status().isConflict());
    }

    @Test
    void nomeRepetidoComOutraCaixaDa422() throws Exception {
        api.criarCartao(CASA, "Cartão A", "4500.00");

        api.postar(CASA, "/cartoes", cartao("cartão a", 1, 10, "100.00"))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Já existe")));
    }

    @Test
    void formatoInvalidoDa400ERegraDa422() throws Exception {
        api.postar(CASA, "/cartoes", cartao("A", 25, 5, "4500")).andExpect(status().isBadRequest());
        api.postar(CASA, "/cartoes", cartao("A", 32, 5, "4500.00")).andExpect(status().is(422));
        api.postar(CASA, "/cartoes", cartao("A", 25, 5, "0.00")).andExpect(status().is(422));
        api.postar(CASA, "/cartoes", cartao(" ", 25, 5, "1.00")).andExpect(status().is(422));
    }

    @Test
    void arquivarComFaturaNaoPagaDa422() throws Exception {
        var id = api.criarCartao(CASA, "Cartão A", "4500.00");
        api.lancarNoCredito(CASA, "10.00", "x", "2026-03-10", id);

        api.apagar(CASA, "/cartoes/" + id).andExpect(status().is(422));
    }

    @Test
    void arquivarSemFaturaNaoPagaSomeDaListaELiberaONome() throws Exception {
        var id = api.criarCartao(CASA, "Cartão A", "4500.00");

        api.apagar(CASA, "/cartoes/" + id).andExpect(status().isNoContent());

        api.get(CASA, "/cartoes").andExpect(jsonPath("$.length()").value(0));
        api.colocar(CASA, "/cartoes/" + id, """
                        {"nome":"x","diaFechamento":10,"diaVencimento":20,"limite":"1.00","versao":1}
                        """)
                .andExpect(status().isNotFound());
        api.criarCartao(CASA, "Cartão A", "100.00");
    }

    @Test
    void gastoNoCartaoArquivadoERecusado() throws Exception {
        var id = api.criarCartao(CASA, "Cartão A", "4500.00");
        api.apagar(CASA, "/cartoes/" + id).andExpect(status().isNoContent());

        api.postar(CASA, "/lancamentos", ApiDeTeste.noCredito("1.00", "x", "2026-03-10", ApiDeTeste.CONFORTO, id))
                .andExpect(status().is(422));
    }

    @Test
    void faturaDaCompraDizOMesEAsDatas() throws Exception {
        var id = api.criarCartao(CASA, "Cartão A", "4500.00");

        api.get(CASA, "/cartoes/" + id + "/fatura-da-compra?data=2026-10-08")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-10"))
                .andExpect(jsonPath("$.fechamento").value("2026-10-25"))
                .andExpect(jsonPath("$.vencimento").value("2026-11-05"))
                .andExpect(jsonPath("$.status").doesNotExist());
        api.get(CASA, "/cartoes/" + id + "/fatura-da-compra?data=2026-10-25").andExpect(jsonPath("$.mes").value("2026-11"));
        api.get(CASA, "/cartoes/" + UUID.randomUUID() + "/fatura-da-compra?data=2026-10-25").andExpect(status().isNotFound());
        api.get(CASA, "/cartoes/" + id + "/fatura-da-compra?data=08/10/2026").andExpect(status().isBadRequest());
    }

    @Test
    void cartaoDaEmpresaNaoApareceNaCasa() throws Exception {
        api.criarCartao(ApiDeTeste.EMPRESA, "Cartão PJ", "1000.00");

        api.get(CASA, "/cartoes").andExpect(jsonPath("$.length()").value(0));
        api.get(ApiDeTeste.EMPRESA, "/cartoes").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void visualizadorLeMasNaoGrava() throws Exception {
        api.criarCartao(CASA, "Cartão A", "4500.00");
        jdbc.sql("UPDATE identity.workspace_member SET role = 'VIEWER' WHERE workspace_id = :w")
                .param("w", UUID.fromString(CASA))
                .update();

        api.get(CASA, "/cartoes").andExpect(status().isOk());
        api.postar(CASA, "/cartoes", cartao("Cartão B", 25, 5, "1.00")).andExpect(status().isForbidden());
    }

    @Test
    void espacoInexistenteDa404() throws Exception {
        api.get(UUID.randomUUID().toString(), "/cartoes").andExpect(status().isNotFound());
    }
}
