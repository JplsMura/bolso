package dev.bolso.lancamentos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.EspacoNaoEncontrado;
import dev.bolso.identidade.Papel;
import dev.bolso.identidade.SemPermissao;
import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.DadosDoLancamento;
import dev.bolso.lancamentos.domain.DadosDoParcelamento;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FaturaFechada;
import dev.bolso.lancamentos.domain.FormaPagamento;
import dev.bolso.lancamentos.domain.Lancamento;
import dev.bolso.lancamentos.domain.LancamentoNaoEncontrado;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Casos de uso de escrita com portas falsas ({@link PortasFalsas}): nada de Spring nem banco. */
class GravarLancamentosTest {

    static final UUID CASA = PortasFalsas.CASA;
    static final UUID EMPRESA = PortasFalsas.EMPRESA;
    static final UUID META = PortasFalsas.META;
    static final UUID TAG = PortasFalsas.TAG;
    static final UUID CARTAO = PortasFalsas.CARTAO;

    PortasFalsas portas;
    Map<UUID, Lancamento> gravados;
    List<UUID> excluidos;
    GravarLancamentos gravar;

    @BeforeEach
    void monta() {
        portas = new PortasFalsas();
        gravados = portas.gravados;
        excluidos = portas.excluidos;
        gravar = portas.gravarLancamentos();
    }

    static DadosDoLancamento gastoNaCasa() {
        return new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("110.00"), "Conta de luz", LocalDate.of(2026, 10, 9), META, FormaPagamento.PIX, Set.of(TAG));
    }

    @Test
    void lancaEDevolveOItemGravado() {
        var item = gravar.lancar(CASA, gastoNaCasa());

        assertThat(item.id().version()).isEqualTo(7);
        assertThat(item.valor()).hasToString("110.00");
        assertThat(item.versao()).isZero();
        assertThat(gravados).containsKey(item.id());
    }

    @Test
    void visualizadorNaoGrava() {
        portas.papel = Papel.VIEWER;

        assertThatThrownBy(() -> gravar.lancar(CASA, gastoNaCasa())).isInstanceOf(SemPermissao.class);
        assertThat(gravados).isEmpty();
    }

    @Test
    void espacoDeOutroUsuarioNaoExiste() {
        assertThatThrownBy(() -> gravar.lancar(UUID.randomUUID(), gastoNaCasa())).isInstanceOf(EspacoNaoEncontrado.class);
    }

    @Test
    void metaDeOutroEspacoERecusada() {
        var dados = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("1.00"), "x", LocalDate.of(2026, 10, 9), UUID.randomUUID(), FormaPagamento.PIX, Set.of());

        assertThatThrownBy(() -> gravar.lancar(CASA, dados)).isInstanceOf(RegraDeNegocio.class).hasMessageContaining("Meta");
    }

    @Test
    void tagDeOutroEspacoERecusada() {
        var dados = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("1.00"), "x", LocalDate.of(2026, 10, 9), META, FormaPagamento.PIX, Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> gravar.lancar(CASA, dados)).isInstanceOf(RegraDeNegocio.class).hasMessageContaining("Tag");
    }

    @Test
    void naEmpresaSaidaSemMetaVale() {
        var dados = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("10.00"), "Nota", LocalDate.of(2026, 10, 9), null, FormaPagamento.BOLETO, Set.of());

        assertThat(gravar.lancar(EMPRESA, dados).metaId()).isNull();
    }

    @Test
    void editaComAVersaoCerta() {
        var criado = gravar.lancar(CASA, gastoNaCasa());
        var novo = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("120.00"), "Luz", LocalDate.of(2026, 10, 10), META, FormaPagamento.DEBIT, Set.of());

        var editado = gravar.editar(CASA, criado.id(), novo, criado.versao());

        assertThat(editado.valor()).hasToString("120.00");
        assertThat(editado.versao()).isEqualTo(1);
    }

    @Test
    void editarComVersaoDesatualizadaDaConflito() {
        var criado = gravar.lancar(CASA, gastoNaCasa());
        gravar.editar(CASA, criado.id(), gastoNaCasa(), criado.versao());

        assertThatThrownBy(() -> gravar.editar(CASA, criado.id(), gastoNaCasa(), criado.versao()))
                .isInstanceOf(ConflitoDeVersao.class);
    }

    @Test
    void editarQueNaoExisteDaNaoEncontrado() {
        assertThatThrownBy(() -> gravar.editar(CASA, UUID.randomUUID(), gastoNaCasa(), 0))
                .isInstanceOf(LancamentoNaoEncontrado.class);
    }

    @Test
    void editarAplicaAsRegrasDeNegocio() {
        var criado = gravar.lancar(CASA, gastoNaCasa());
        var invalido = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("1.00"), "x", LocalDate.of(2026, 10, 9), null, FormaPagamento.PIX, Set.of());

        assertThatThrownBy(() -> gravar.editar(CASA, criado.id(), invalido, criado.versao())).isInstanceOf(RegraDeNegocio.class);
    }

    @Test
    void excluiEDepoisNaoEncontra() {
        var criado = gravar.lancar(CASA, gastoNaCasa());

        gravar.excluir(CASA, criado.id());

        assertThat(excluidos).containsExactly(criado.id());
        assertThatThrownBy(() -> gravar.excluir(CASA, criado.id())).isInstanceOf(LancamentoNaoEncontrado.class);
    }

    @Test
    void visualizadorNaoExclui() {
        var criado = gravar.lancar(CASA, gastoNaCasa());
        portas.papel = Papel.VIEWER;

        assertThatThrownBy(() -> gravar.excluir(CASA, criado.id())).isInstanceOf(SemPermissao.class);
        assertThat(gravados).containsKey(criado.id());
    }

    @Test
    void lancamentoDeOutroEspacoNaoEEditavelPorAqui() {
        var criado = gravar.lancar(CASA, gastoNaCasa());

        assertThatThrownBy(() -> gravar.excluir(EMPRESA, criado.id())).isInstanceOf(LancamentoNaoEncontrado.class);
    }

    // ---------- cartão de crédito (004) ----------

    static DadosDoLancamento noCartao(String valor, String descricao, LocalDate data, UUID cartao) {
        return new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto(valor), descricao, data, META, FormaPagamento.CREDIT, cartao, Set.of());
    }

    @Test
    void compraAntesDoFechamentoCaiNaFaturaDoMesENoDiaDoFechamentoNaSeguinte() {
        var antes = gravar.lancar(CASA, noCartao("23.90", "Uber", LocalDate.of(2026, 10, 24), CARTAO));
        var noDia = gravar.lancar(CASA, noCartao("39.90", "Streaming", LocalDate.of(2026, 10, 25), CARTAO));

        assertThat(portas.mesDa(antes.faturaId())).isEqualTo(YearMonth.of(2026, 10));
        assertThat(portas.mesDa(noDia.faturaId())).isEqualTo(YearMonth.of(2026, 11));
        assertThat(antes.faturaMes()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(noDia.mesReferencia()).isEqualTo(YearMonth.of(2026, 10)); // o orçamento conta pela data da compra
    }

    @Test
    void cartaoDeOutroEspacoERecusado() {
        assertThatThrownBy(() -> gravar.lancar(CASA, noCartao("1.00", "x", LocalDate.of(2026, 10, 8), PortasFalsas.CARTAO_DA_EMPRESA)))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessageContaining("Cartão não encontrado");
        assertThat(gravados).isEmpty();
    }

    @Test
    void compraEmFaturaFechadaERecusada() {
        portas.fechar(CARTAO, YearMonth.of(2026, 10));

        assertThatThrownBy(() -> gravar.lancar(CASA, noCartao("1.00", "x", LocalDate.of(2026, 10, 8), CARTAO)))
                .isInstanceOf(FaturaFechada.class)
                .hasMessageContaining("outubro/2026")
                .hasMessageContaining("Reabra");
    }

    @Test
    void emFaturaFechadaDescricaoMetaETagsMudamMasValorDataCartaoEExclusaoNao() {
        var criado = gravar.lancar(CASA, noCartao("23.90", "Uber", LocalDate.of(2026, 10, 8), CARTAO));
        portas.fechar(CARTAO, YearMonth.of(2026, 10));

        var soDescricao = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("23.90"), "Uber para o centro", LocalDate.of(2026, 10, 8), META,
                FormaPagamento.CREDIT, CARTAO, Set.of(TAG));
        var editado = gravar.editar(CASA, criado.id(), soDescricao, criado.versao());
        assertThat(editado.descricao()).isEqualTo("Uber para o centro");
        assertThat(editado.faturaId()).isEqualTo(criado.faturaId());

        var outroValor = noCartao("30.00", "Uber", LocalDate.of(2026, 10, 8), CARTAO);
        var paraPix = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("23.90"), "Uber", LocalDate.of(2026, 10, 8), META, FormaPagamento.PIX, Set.of());
        assertThatThrownBy(() -> gravar.editar(CASA, criado.id(), outroValor, editado.versao())).isInstanceOf(FaturaFechada.class);
        assertThatThrownBy(() -> gravar.editar(CASA, criado.id(), paraPix, editado.versao())).isInstanceOf(FaturaFechada.class);
        assertThatThrownBy(() -> gravar.excluir(CASA, criado.id())).isInstanceOf(FaturaFechada.class);
        assertThat(gravados).containsKey(criado.id());
    }

    @Test
    void mudarADataLevaOGastoParaOutraFaturaAberta() {
        var criado = gravar.lancar(CASA, noCartao("23.90", "Uber", LocalDate.of(2026, 10, 8), CARTAO));

        var editado = gravar.editar(CASA, criado.id(), noCartao("23.90", "Uber", LocalDate.of(2026, 10, 28), CARTAO), criado.versao());

        assertThat(portas.mesDa(editado.faturaId())).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    void trocarCreditoPorPixTiraOGastoDaFatura() {
        var criado = gravar.lancar(CASA, noCartao("23.90", "Uber", LocalDate.of(2026, 10, 8), CARTAO));

        var editado = gravar.editar(CASA, criado.id(), gastoNaCasa(), criado.versao());

        assertThat(editado.cartaoId()).isNull();
        assertThat(editado.faturaId()).isNull();
    }

    @Test
    void parcelaNaoSeEditaNemSeExcluiPorAqui() {
        var parcelamento = portas.gravarParcelamentos().criar(CASA, new DadosDoParcelamento(
                Dinheiro.deTexto("30.00"), 3, 1, "Curso", LocalDate.of(2026, 10, 3), CARTAO, META, Set.of()));
        var parcela = parcelamento.parcelas().getFirst().item();

        assertThatThrownBy(() -> gravar.editar(CASA, parcela.id(), gastoNaCasa(), parcela.versao()))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessageContaining("parcelamento");
        assertThatThrownBy(() -> gravar.excluir(CASA, parcela.id())).hasMessageContaining("parcelamento");
    }

    @Test
    void naEmpresaGastoNoCartaoDaEmpresaVale() {
        var dados = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("10.00"), "Domínio", LocalDate.of(2026, 10, 9), null, FormaPagamento.CREDIT,
                PortasFalsas.CARTAO_DA_EMPRESA, Set.of());

        assertThat(gravar.lancar(EMPRESA, dados).faturaId()).isNotNull();
    }
}
