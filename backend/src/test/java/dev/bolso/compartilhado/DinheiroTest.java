package dev.bolso.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DinheiroTest {

    @Test
    void leOTextoDaApiComDuasCasas() {
        assertThat(Dinheiro.deTexto("110.00").valor()).isEqualByComparingTo("110");
        assertThat(Dinheiro.deTexto("110.00")).hasToString("110.00");
        assertThat(Dinheiro.deTexto("0.05")).hasToString("0.05");
    }

    @ParameterizedTest
    @ValueSource(strings = {"110", "110.5", "110.555", "-1.00", "1e3", "1,00", " 1.00", "", "abc", "1234567890123.00"})
    void recusaTextoForaDoFormato(String texto) {
        assertThatThrownBy(() -> Dinheiro.deTexto(texto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recusaNulo() {
        assertThatThrownBy(() -> Dinheiro.deTexto(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroEValidoMasNaoEPositivo() {
        // a sobra do mês pode ser zero; quem exige valor positivo (o lançamento) confere por ehPositivo()
        assertThat(Dinheiro.deTexto("0.00").ehPositivo()).isFalse();
        assertThat(Dinheiro.ZERO).hasToString("0.00");
    }

    @Test
    void somaESubtracaoMantemDuasCasasEPodemFicarNegativas() {
        var a = Dinheiro.deTexto("0.10");
        var b = Dinheiro.deTexto("0.20");

        assertThat(a.somar(b)).hasToString("0.30");
        assertThat(a.subtrair(b)).hasToString("-0.10");
        assertThat(a.subtrair(b).ehPositivo()).isFalse();
    }

    @Test
    void doBancoAceitaEscalaMenorMasNuncaPerdeCentavos() {
        assertThat(Dinheiro.deNumerico(new BigDecimal("5"))).hasToString("5.00");
        assertThatThrownBy(() -> Dinheiro.deNumerico(new BigDecimal("5.001"))).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void construtorExigeExatamenteDuasCasas() {
        assertThatThrownBy(() -> new Dinheiro(new BigDecimal("1.5"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ordenaPorValor() {
        assertThat(Dinheiro.deTexto("2.00")).isGreaterThan(Dinheiro.deTexto("1.99"));
    }
}
