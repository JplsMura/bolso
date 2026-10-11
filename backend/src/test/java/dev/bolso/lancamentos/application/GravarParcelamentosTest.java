package dev.bolso.lancamentos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.Papel;
import dev.bolso.identidade.SemPermissao;
import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.DadosDoParcelamento;
import dev.bolso.lancamentos.domain.FaturaFechada;
import dev.bolso.lancamentos.domain.Lancamento;
import dev.bolso.lancamentos.domain.ParcelamentoNaoEncontrado;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Parcelamento com portas falsas: parcelas, faturas, edição e exclusão. Valores fictícios. */
class GravarParcelamentosTest {

    static final UUID CASA = PortasFalsas.CASA;
    static final UUID META = PortasFalsas.META;
    static final UUID TAG = PortasFalsas.TAG;
    static final UUID CARTAO = PortasFalsas.CARTAO;

    PortasFalsas portas;
    GravarParcelamentos gravar;

    @BeforeEach
    void monta() {
        portas = new PortasFalsas();
        gravar = portas.gravarParcelamentos();
    }

    static DadosDoParcelamento curso(int parcelas, int atual, LocalDate data) {
        return new DadosDoParcelamento(Dinheiro.deTexto("150.00"), parcelas, atual, "Curso online", data, CARTAO, META, Set.of(TAG));
    }

    @Test
    void criaUmaParcelaPorFaturaComNumeroEMesDoOrcamento() {
        var visao = gravar.criar(CASA, curso(6, 1, LocalDate.of(2026, 10, 3)));

        assertThat(visao.parcelas()).hasSize(6);
        assertThat(visao.parcelas()).extracting(p -> p.item().parcela().numero()).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(visao.parcelas()).extracting(p -> portas.mesDa(p.item().faturaId())).containsExactly(
                YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2026, 12),
                YearMonth.of(2027, 1), YearMonth.of(2027, 2), YearMonth.of(2027, 3));
        assertThat(visao.parcelas()).extracting(p -> p.item().mesReferencia()).containsExactly(
                YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2026, 12),
                YearMonth.of(2027, 1), YearMonth.of(2027, 2), YearMonth.of(2027, 3));
        assertThat(visao.parcelas()).allSatisfy(p -> {
            assertThat(p.item().valor()).hasToString("25.00");
            assertThat(p.item().parcela().total()).isEqualTo(6);
            assertThat(p.faturaAberta()).isTrue();
        });
        assertThat(visao.metaId()).isEqualTo(META);
    }

    @Test
    void compraDepoisDoFechamentoComecaNaFaturaSeguinte() {
        var visao = gravar.criar(CASA, curso(2, 1, LocalDate.of(2026, 10, 26)));

        assertThat(visao.parcelas()).extracting(p -> portas.mesDa(p.item().faturaId()))
                .containsExactly(YearMonth.of(2026, 11), YearMonth.of(2026, 12));
    }

    @Test
    void emAndamentoCriaSoAsQueFaltam() {
        var visao = gravar.criar(CASA, curso(6, 4, LocalDate.of(2026, 7, 3)));

        assertThat(visao.parcelas()).extracting(p -> p.item().parcela().numero()).containsExactly(4, 5, 6);
        assertThat(visao.parcelas()).extracting(p -> portas.mesDa(p.item().faturaId()))
                .containsExactly(YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2026, 12));
    }

    @Test
    void parcelaQueCairiaEmFaturaFechadaRecusaTudo() {
        portas.fechar(CARTAO, YearMonth.of(2026, 11));

        assertThatThrownBy(() -> gravar.criar(CASA, curso(3, 1, LocalDate.of(2026, 10, 3)))).isInstanceOf(FaturaFechada.class);
    }

    @Test
    void metaDaCasaContinuaObrigatoria() {
        var semMeta = new DadosDoParcelamento(
                Dinheiro.deTexto("10.00"), 2, 1, "x", LocalDate.of(2026, 10, 3), CARTAO, null, Set.of());

        assertThatThrownBy(() -> gravar.criar(CASA, semMeta)).isInstanceOf(RegraDeNegocio.class).hasMessageContaining("meta");
        assertThat(portas.gravados).isEmpty();
    }

    @Test
    void cartaoDeOutroEspacoERecusado() {
        var outro = new DadosDoParcelamento(
                Dinheiro.deTexto("10.00"), 2, 1, "x", LocalDate.of(2026, 10, 3), PortasFalsas.CARTAO_DA_EMPRESA, META, Set.of());

        assertThatThrownBy(() -> gravar.criar(CASA, outro)).hasMessageContaining("Cartão não encontrado");
    }

    @Test
    void visualizadorNaoParcela() {
        portas.papel = Papel.VIEWER;

        assertThatThrownBy(() -> gravar.criar(CASA, curso(2, 1, LocalDate.of(2026, 10, 3)))).isInstanceOf(SemPermissao.class);
    }

    @Test
    void editarMudaSoAsParcelasEmFaturaAberta() {
        var visao = gravar.criar(CASA, curso(3, 1, LocalDate.of(2026, 10, 3)));
        portas.fechar(CARTAO, YearMonth.of(2026, 10));

        var editada = gravar.editar(CASA, visao.parcelamento().id(), "Curso de Java", META, Set.of(), 0);

        assertThat(editada.parcelamento().descricao()).isEqualTo("Curso de Java");
        assertThat(editada.parcelamento().versao()).isEqualTo(1);
        assertThat(portas.gravados.values()).extracting(Lancamento::descricao)
                .containsExactly("Curso online", "Curso de Java", "Curso de Java");
        assertThat(editada.parcelas()).extracting(ParcelaDoParcelamento::faturaAberta).containsExactly(false, true, true);
    }

    @Test
    void editarComVersaoVelhaDaConflito() {
        var visao = gravar.criar(CASA, curso(3, 1, LocalDate.of(2026, 10, 3)));
        gravar.editar(CASA, visao.parcelamento().id(), "Curso de Java", META, Set.of(), 0);

        assertThatThrownBy(() -> gravar.editar(CASA, visao.parcelamento().id(), "Outro", META, Set.of(), 0))
                .isInstanceOf(ConflitoDeVersao.class);
    }

    @Test
    void excluirTiraSoAsParcelasEmFaturaAbertaEMantemOParcelamento() {
        var visao = gravar.criar(CASA, curso(3, 1, LocalDate.of(2026, 10, 3)));
        var id = visao.parcelamento().id();
        portas.fechar(CARTAO, YearMonth.of(2026, 10));

        gravar.excluir(CASA, id);

        assertThat(portas.gravados.values()).extracting(l -> l.parcela().numero()).containsExactly(1);
        assertThat(portas.parcelamentosExcluidos).doesNotContain(id);
        assertThatThrownBy(() -> gravar.excluir(CASA, id)).isInstanceOf(RegraDeNegocio.class).hasMessageContaining("fechadas");
    }

    @Test
    void excluirTudoApagaOParcelamento() {
        var id = gravar.criar(CASA, curso(3, 1, LocalDate.of(2026, 10, 3))).parcelamento().id();

        gravar.excluir(CASA, id);

        assertThat(portas.gravados).isEmpty();
        assertThat(portas.parcelamentosExcluidos).contains(id);
        assertThatThrownBy(() -> gravar.excluir(CASA, id)).isInstanceOf(ParcelamentoNaoEncontrado.class);
    }
}
