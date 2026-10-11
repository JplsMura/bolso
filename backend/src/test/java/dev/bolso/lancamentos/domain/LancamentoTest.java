package dev.bolso.lancamentos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.TipoEspaco;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LancamentoTest {

    static final UUID ID = UUID.fromString("00000000-0000-7000-8000-000000000001");
    static final UUID ESPACO = UUID.fromString("00000000-0000-7000-8000-0000000000c1");
    static final UUID META = UUID.fromString("00000000-0000-7000-8000-0000000000a1");
    static final LocalDate DATA = LocalDate.of(2026, 10, 9);

    static DadosDoLancamento saida(Dinheiro valor, String descricao, UUID meta, FormaPagamento forma, Set<UUID> tags) {
        return new DadosDoLancamento(Direcao.OUT, valor, descricao, DATA, meta, forma, tags);
    }

    static DadosDoLancamento saidaValida() {
        return saida(Dinheiro.deTexto("110.00"), "Conta de luz", META, FormaPagamento.PIX, Set.of());
    }

    static void recusa(TipoEspaco tipo, DadosDoLancamento dados, String trecho) {
        assertThatThrownBy(() -> Lancamento.novo(ID, ESPACO, tipo, dados))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessageContaining(trecho);
    }

    @Test
    void saidaValidaNaCasaViraLancamentoNaVersaoZero() {
        var l = Lancamento.novo(ID, ESPACO, TipoEspaco.HOME, saidaValida());

        assertThat(l.direcao()).isEqualTo(Direcao.OUT);
        assertThat(l.valor()).hasToString("110.00");
        assertThat(l.metaId()).isEqualTo(META);
        assertThat(l.versao()).isZero();
        assertThat(l.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void mesDeReferenciaVemDaData() {
        var dados = new DadosDoLancamento(Direcao.IN, Dinheiro.deTexto("1.00"), "Pix", LocalDate.of(2026, 1, 31), null, null, Set.of());

        assertThat(Lancamento.novo(ID, ESPACO, TipoEspaco.HOME, dados).mesReferencia()).isEqualTo(YearMonth.of(2026, 1));
    }

    @Test
    void entradaValida() {
        var dados = new DadosDoLancamento(Direcao.IN, Dinheiro.deTexto("5000.00"), "Salário", DATA, null, null, Set.of());

        assertThat(Lancamento.novo(ID, ESPACO, TipoEspaco.HOME, dados).metaId()).isNull();
    }

    @Test
    void valorZeroNaoVale() {
        recusa(TipoEspaco.HOME, saida(Dinheiro.ZERO, "x", META, FormaPagamento.PIX, Set.of()), "maior que zero");
    }

    @Test
    void descricaoEmBrancoNaoVale() {
        recusa(TipoEspaco.HOME, saida(Dinheiro.deTexto("1.00"), "   ", META, FormaPagamento.PIX, Set.of()), "descrição");
    }

    @Test
    void descricaoEhAparadaEAceitaAte140() {
        var cento40 = "a".repeat(140);
        var l = Lancamento.novo(ID, ESPACO, TipoEspaco.HOME, saida(Dinheiro.deTexto("1.00"), "  " + cento40 + "  ", META, FormaPagamento.PIX, Set.of()));

        assertThat(l.descricao()).isEqualTo(cento40);
        recusa(TipoEspaco.HOME, saida(Dinheiro.deTexto("1.00"), "a".repeat(141), META, FormaPagamento.PIX, Set.of()), "140");
    }

    @Test
    void naCasaTodaSaidaPrecisaDeMeta() {
        recusa(TipoEspaco.HOME, saida(Dinheiro.deTexto("1.00"), "x", null, FormaPagamento.PIX, Set.of()), "meta");
    }

    @Test
    void naEmpresaSaidaNaoTemMeta() {
        recusa(TipoEspaco.COMPANY, saida(Dinheiro.deTexto("1.00"), "x", META, FormaPagamento.PIX, Set.of()), "Empresa");
        var l = Lancamento.novo(ID, ESPACO, TipoEspaco.COMPANY, saida(Dinheiro.deTexto("1.00"), "x", null, FormaPagamento.PIX, Set.of()));
        assertThat(l.metaId()).isNull();
    }

    @Test
    void entradaNaoTemMetaNemFormaDePagamento() {
        var comMeta = new DadosDoLancamento(Direcao.IN, Dinheiro.deTexto("1.00"), "x", DATA, META, null, Set.of());
        var comForma = new DadosDoLancamento(Direcao.IN, Dinheiro.deTexto("1.00"), "x", DATA, null, FormaPagamento.PIX, Set.of());

        recusa(TipoEspaco.HOME, comMeta, "Entrada");
        recusa(TipoEspaco.HOME, comForma, "Entrada");
    }

    @Test
    void saidaPrecisaDeFormaDePagamento() {
        recusa(TipoEspaco.HOME, saida(Dinheiro.deTexto("1.00"), "x", META, null, Set.of()), "forma de pagamento");
    }

    static final UUID CARTAO = UUID.fromString("00000000-0000-7000-8000-0000000000d1");
    static final UUID FATURA = UUID.fromString("00000000-0000-7000-8000-0000000000e1");

    static DadosDoLancamento noCredito(UUID cartao) {
        return new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("23.90"), "Uber", DATA, META, FormaPagamento.CREDIT, cartao, Set.of());
    }

    @Test
    void creditoPrecisaDeCartao() {
        recusa(TipoEspaco.HOME, noCredito(null), "cartão");
    }

    @Test
    void cartaoSoComCredito() {
        var pix = new DadosDoLancamento(Direcao.OUT, Dinheiro.deTexto("1.00"), "x", DATA, META, FormaPagamento.PIX, CARTAO, Set.of());
        recusa(TipoEspaco.HOME, pix, "Cartão só vale");
    }

    @Test
    void entradaNaoTemCartao() {
        var entrada = new DadosDoLancamento(Direcao.IN, Dinheiro.deTexto("1.00"), "x", DATA, null, null, CARTAO, Set.of());
        recusa(TipoEspaco.HOME, entrada, "Entrada");
    }

    @Test
    void creditoComCartaoValeEEntraNaFatura() {
        var l = Lancamento.novo(ID, ESPACO, TipoEspaco.HOME, noCredito(CARTAO)).naFatura(FATURA);

        assertThat(l.cartaoId()).isEqualTo(CARTAO);
        assertThat(l.faturaId()).isEqualTo(FATURA);
        assertThat(l.ehParcela()).isFalse();
    }

    @Test
    void soGastoNoCartaoTemFaturaOuParcela() {
        var pix = Lancamento.novo(ID, ESPACO, TipoEspaco.HOME, saidaValida());

        assertThatThrownBy(() -> pix.naFatura(FATURA)).isInstanceOf(RegraDeNegocio.class);
        assertThatThrownBy(() -> pix.comoParcela(new Parcela(UUID.randomUUID(), 1, 3))).isInstanceOf(RegraDeNegocio.class);
    }

    @Test
    void editarMantemAFaturaETrocarParaPixTiraACompraDoCartao() {
        var l = Lancamento.novo(ID, ESPACO, TipoEspaco.HOME, noCredito(CARTAO)).naFatura(FATURA);

        assertThat(l.editar(TipoEspaco.HOME, noCredito(CARTAO)).faturaId()).isEqualTo(FATURA);
        var pix = l.editar(TipoEspaco.HOME, saidaValida());
        assertThat(pix.cartaoId()).isNull();
        assertThat(pix.faturaId()).isNull();
    }

    @Test
    void soValorDataDirecaoFormaECartaoMexemNaFatura() {
        var antes = Lancamento.novo(ID, ESPACO, TipoEspaco.HOME, noCredito(CARTAO));
        var d = antes.dados();

        var outraDescricaoMetaETags = new DadosDoLancamento(
                d.direcao(), d.valor(), "Outra", d.data(), UUID.randomUUID(), d.formaPagamento(), d.cartaoId(), Set.of(UUID.randomUUID()));
        assertThat(antes.editar(TipoEspaco.HOME, outraDescricaoMetaETags).mexeNaFaturaEmRelacaoA(antes)).isFalse();

        var outroValor = new DadosDoLancamento(
                d.direcao(), Dinheiro.deTexto("24.00"), d.descricao(), d.data(), d.metaId(), d.formaPagamento(), d.cartaoId(), d.tagIds());
        var outraData = new DadosDoLancamento(
                d.direcao(), d.valor(), d.descricao(), DATA.plusDays(1), d.metaId(), d.formaPagamento(), d.cartaoId(), d.tagIds());
        var outroCartao = new DadosDoLancamento(
                d.direcao(), d.valor(), d.descricao(), d.data(), d.metaId(), d.formaPagamento(), UUID.randomUUID(), d.tagIds());
        assertThat(antes.editar(TipoEspaco.HOME, outroValor).mexeNaFaturaEmRelacaoA(antes)).isTrue();
        assertThat(antes.editar(TipoEspaco.HOME, outraData).mexeNaFaturaEmRelacaoA(antes)).isTrue();
        assertThat(antes.editar(TipoEspaco.HOME, outroCartao).mexeNaFaturaEmRelacaoA(antes)).isTrue();
        assertThat(antes.editar(TipoEspaco.HOME, saidaValida()).mexeNaFaturaEmRelacaoA(antes)).isTrue();
    }

    @Test
    void ateDezTags() {
        Set<UUID> onze = new HashSet<>();
        for (int i = 0; i < 11; i++) {
            onze.add(UUID.randomUUID());
        }
        var dez = Set.copyOf(onze.stream().limit(10).toList());

        assertThat(Lancamento.novo(ID, ESPACO, TipoEspaco.HOME, saida(Dinheiro.deTexto("1.00"), "x", META, FormaPagamento.PIX, dez)).tagIds()).hasSize(10);
        recusa(TipoEspaco.HOME, saida(Dinheiro.deTexto("1.00"), "x", META, FormaPagamento.PIX, onze), "10 tags");
    }

    @Test
    void editarAplicaAsMesmasRegrasEMantemIdEVersao() {
        var original = Lancamento.reconstituir(ID, ESPACO, saidaValida(), 3);

        var editado = original.editar(TipoEspaco.HOME, saida(Dinheiro.deTexto("200.00"), "Outra", META, FormaPagamento.DEBIT, Set.of()));

        assertThat(editado.id()).isEqualTo(ID);
        assertThat(editado.versao()).isEqualTo(3);
        assertThat(editado.valor()).hasToString("200.00");
        assertThatThrownBy(() -> original.editar(TipoEspaco.HOME, saida(Dinheiro.ZERO, "x", META, FormaPagamento.PIX, Set.of())))
                .isInstanceOf(RegraDeNegocio.class);
    }
}
