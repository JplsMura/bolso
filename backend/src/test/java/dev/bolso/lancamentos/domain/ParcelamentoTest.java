package dev.bolso.lancamentos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.compartilhado.Dinheiro;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Divisão dos centavos, datas e a parcela atual. Valores fictícios. */
class ParcelamentoTest {

    static final UUID ID = UUID.fromString("00000000-0000-7000-8000-0000000000f1");
    static final UUID ESPACO = UUID.fromString("00000000-0000-7000-8000-0000000000c1");
    static final UUID CARTAO = UUID.fromString("00000000-0000-7000-8000-0000000000d1");

    static DadosDoParcelamento dados(String total, int parcelas, int atual, LocalDate data) {
        return new DadosDoParcelamento(Dinheiro.deTexto(total), parcelas, atual, "Curso online", data, CARTAO, null, Set.of());
    }

    static Parcelamento parcelamento(String total, int parcelas, int atual, LocalDate data) {
        return Parcelamento.novo(ID, ESPACO, dados(total, parcelas, atual, data));
    }

    @Test
    void sobraDeCentavosVaiNaPrimeiraESomaBateComOTotal() {
        var parcelas = parcelamento("100.00", 3, 1, LocalDate.of(2026, 10, 3)).parcelasACriar();

        assertThat(parcelas).extracting(p -> p.valor().toString()).containsExactly("33.34", "33.33", "33.33");
        assertThat(parcelas.stream().map(ParcelaPlanejada::valor).reduce(Dinheiro.ZERO, Dinheiro::somar)).hasToString("100.00");
    }

    @Test
    void divisaoExataNaoMexeEmNada() {
        var parcelas = parcelamento("150.00", 6, 1, LocalDate.of(2026, 10, 3)).parcelasACriar();

        assertThat(parcelas).hasSize(6).allSatisfy(p -> assertThat(p.valor()).hasToString("25.00"));
    }

    @Test
    void cadaParcelaContaNoMesDelaECaiNaFaturaSeguinte() {
        var parcelas = parcelamento("150.00", 6, 1, LocalDate.of(2026, 10, 3)).parcelasACriar();

        assertThat(parcelas).extracting(ParcelaPlanejada::data).containsExactly(
                LocalDate.of(2026, 10, 3), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 12, 3),
                LocalDate.of(2027, 1, 3), LocalDate.of(2027, 2, 3), LocalDate.of(2027, 3, 3));
        assertThat(parcelas).extracting(ParcelaPlanejada::mesesDepoisDaPrimeira).containsExactly(0, 1, 2, 3, 4, 5);
    }

    @Test
    void dia31ViraOUltimoDiaNosMesesCurtos() {
        var parcelas = parcelamento("40.00", 4, 1, LocalDate.of(2027, 1, 31)).parcelasACriar();

        assertThat(parcelas).extracting(ParcelaPlanejada::data).containsExactly(
                LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31), LocalDate.of(2027, 4, 30));
    }

    @Test
    void emAndamentoCriaSoDaParcelaAtualEmDiante() {
        var parcelas = parcelamento("150.00", 6, 4, LocalDate.of(2026, 7, 3)).parcelasACriar();

        assertThat(parcelas).extracting(ParcelaPlanejada::numero).containsExactly(4, 5, 6);
        assertThat(parcelas).extracting(ParcelaPlanejada::data)
                .containsExactly(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 12, 3));
        assertThat(parcelas).extracting(ParcelaPlanejada::mesesDepoisDaPrimeira).containsExactly(3, 4, 5);
    }

    @Test
    void emAndamentoNaoLevaOsCentavosDaPrimeira() {
        var parcelas = parcelamento("100.00", 3, 2, LocalDate.of(2026, 9, 3)).parcelasACriar();

        assertThat(parcelas).extracting(p -> p.valor().toString()).containsExactly("33.33", "33.33");
    }

    @Test
    void recusaMenosDeUmCentavoPorParcela() {
        assertThatThrownBy(() -> parcelamento("0.05", 6, 1, LocalDate.of(2026, 10, 3)))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessageContaining("R$ 0,01");
    }

    @Test
    void parcelasDe2A48() {
        assertThatThrownBy(() -> parcelamento("10.00", 1, 1, LocalDate.of(2026, 10, 3))).isInstanceOf(RegraDeNegocio.class);
        assertThatThrownBy(() -> parcelamento("100.00", 49, 1, LocalDate.of(2026, 10, 3))).isInstanceOf(RegraDeNegocio.class);
        assertThat(parcelamento("100.00", 48, 1, LocalDate.of(2026, 10, 3)).parcelasACriar()).hasSize(48);
    }

    @Test
    void parcelaAtualDentroDoTotal() {
        assertThatThrownBy(() -> parcelamento("10.00", 3, 4, LocalDate.of(2026, 10, 3))).isInstanceOf(RegraDeNegocio.class);
        assertThatThrownBy(() -> parcelamento("10.00", 3, 0, LocalDate.of(2026, 10, 3))).isInstanceOf(RegraDeNegocio.class);
    }

    @Test
    void precisaDeCartao() {
        var semCartao = new DadosDoParcelamento(
                Dinheiro.deTexto("10.00"), 2, 1, "x", LocalDate.of(2026, 10, 3), null, null, Set.of());

        assertThatThrownBy(() -> Parcelamento.novo(ID, ESPACO, semCartao)).hasMessageContaining("cartão");
    }

    @Test
    void descricaoEhAparadaETrocadaComAsMesmasRegras() {
        var p = Parcelamento.novo(ID, ESPACO, new DadosDoParcelamento(
                Dinheiro.deTexto("10.00"), 2, 1, "  Curso  ", LocalDate.of(2026, 10, 3), CARTAO, null, Set.of()));

        assertThat(p.descricao()).isEqualTo("Curso");
        assertThat(p.comDescricao(" Curso de Java ").descricao()).isEqualTo("Curso de Java");
        assertThatThrownBy(() -> p.comDescricao("  ")).isInstanceOf(RegraDeNegocio.class);
    }
}
