package dev.bolso.cartoes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** A regra de datas mais sensível do sistema: em qual fatura cai a compra, e quando ela fecha e vence. */
class CalendarioDoCartaoTest {

    @ParameterizedTest(name = "fecha dia {0}: compra em {1} cai na fatura de {2}")
    @CsvSource({
        "25, 2026-10-24, 2026-10", // um dia antes do fechamento
        "25, 2026-10-25, 2026-11", // no dia do fechamento: seguinte
        "25, 2026-10-31, 2026-11",
        "5,  2026-10-01, 2026-10",
        "5,  2026-10-05, 2026-11",
        "31, 2027-02-27, 2027-02", // fevereiro fecha no dia 28
        "31, 2027-02-28, 2027-03",
        "31, 2028-02-28, 2028-02", // ano bissexto: fecha no 29
        "31, 2028-02-29, 2028-03",
        "30, 2026-12-30, 2027-01", // virada de ano
        "1,  2026-10-01, 2026-11"
    })
    void mesDaFatura(int diaFechamento, LocalDate compra, String mes) {
        assertThat(new CalendarioDoCartao(diaFechamento, 10).mesDaFatura(compra)).isEqualTo(YearMonth.parse(mes));
    }

    @ParameterizedTest(name = "fecha {0}, vence {1}: fatura de {2} fecha {3} e vence {4}")
    @CsvSource({
        "25, 5,  2026-10, 2026-10-25, 2026-11-05", // vencimento menor: mês seguinte
        "5,  15, 2026-10, 2026-10-05, 2026-10-15", // vencimento maior: mesmo mês
        "10, 10, 2026-10, 2026-10-10, 2026-11-10", // mesmo dia: mês seguinte
        "31, 10, 2027-02, 2027-02-28, 2027-03-10",
        "30, 31, 2027-02, 2027-02-28, 2027-03-31", // os dois viram 28 em fevereiro: vence no mês seguinte
        "29, 30, 2026-04, 2026-04-29, 2026-04-30",
        "25, 5,  2026-12, 2026-12-25, 2027-01-05"  // virada de ano
    })
    void fechamentoEVencimento(int diaFechamento, int diaVencimento, String mes, LocalDate fechamento, LocalDate vencimento) {
        var calendario = new CalendarioDoCartao(diaFechamento, diaVencimento);

        assertThat(calendario.fechamento(YearMonth.parse(mes))).isEqualTo(fechamento);
        assertThat(calendario.vencimento(YearMonth.parse(mes))).isEqualTo(vencimento);
    }

    @Test
    void diasDe1A31() {
        assertThatThrownBy(() -> new CalendarioDoCartao(0, 10)).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> new CalendarioDoCartao(32, 10)).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> new CalendarioDoCartao(10, 0)).isInstanceOf(RegraDoCartao.class);
    }
}
