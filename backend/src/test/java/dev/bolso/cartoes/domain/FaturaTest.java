package dev.bolso.cartoes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.compartilhado.Dinheiro;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Ciclo da fatura: aberta → fechada → paga, e as voltas. Valores fictícios. */
class FaturaTest {

    static final Cartao CARTAO = Cartao.novo(
            UUID.fromString("00000000-0000-7000-8000-0000000000d1"),
            UUID.fromString("00000000-0000-7000-8000-0000000000c1"),
            new DadosDoCartao("Cartão A", 25, 5, Dinheiro.deTexto("4500.00")));
    static final LocalDate FECHAMENTO = LocalDate.of(2026, 10, 25);

    static Fatura aberta() {
        return Fatura.nova(UUID.fromString("00000000-0000-7000-8000-0000000000e1"), CARTAO, YearMonth.of(2026, 10));
    }

    static Fatura fechada() {
        return aberta().fechar(FECHAMENTO, Dinheiro.deTexto("1260.00"));
    }

    @Test
    void novaNasceAbertaComAsDatasDoCartao() {
        var f = aberta();

        assertThat(f.status()).isEqualTo(StatusDaFatura.OPEN);
        assertThat(f.fechamento()).isEqualTo(FECHAMENTO);
        assertThat(f.vencimento()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(f.totalCongelado()).isNull();
        assertThat(f.total(Dinheiro.deTexto("10.00"))).hasToString("10.00"); // aberta: soma ao vivo
    }

    @Test
    void fecharCongelaOTotal() {
        var f = fechada();

        assertThat(f.status()).isEqualTo(StatusDaFatura.CLOSED);
        assertThat(f.totalCongelado()).hasToString("1260.00");
        assertThat(f.total(Dinheiro.deTexto("99.00"))).hasToString("1260.00");
    }

    @Test
    void naoFechaAntesDaDataDeFechamento() {
        assertThatThrownBy(() -> aberta().fechar(FECHAMENTO.minusDays(1), Dinheiro.ZERO))
                .isInstanceOf(RegraDoCartao.class)
                .hasMessageContaining("25/10/2026");
    }

    @Test
    void pagarGuardaAData() {
        var paga = fechada().pagar(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 4));

        assertThat(paga.status()).isEqualTo(StatusDaFatura.PAID);
        assertThat(paga.pagaEm()).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(paga.totalCongelado()).hasToString("1260.00");
    }

    @Test
    void pagamentoNaoPodeSerAntesDoFechamentoNemNoFuturo() {
        var hoje = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> fechada().pagar(FECHAMENTO.minusDays(1), hoje)).hasMessageContaining("antes do fechamento");
        assertThatThrownBy(() -> fechada().pagar(hoje.plusDays(1), hoje)).hasMessageContaining("futuro");
        assertThat(fechada().pagar(FECHAMENTO, hoje).pagaEm()).isEqualTo(FECHAMENTO);
    }

    @Test
    void reabrirLimpaOTotal() {
        var reaberta = fechada().reabrir();

        assertThat(reaberta.status()).isEqualTo(StatusDaFatura.OPEN);
        assertThat(reaberta.totalCongelado()).isNull();
    }

    @Test
    void desfazerPagamentoVoltaParaFechada() {
        var hoje = LocalDate.of(2026, 11, 4);
        var f = fechada().pagar(hoje, hoje).desfazerPagamento();

        assertThat(f.status()).isEqualTo(StatusDaFatura.CLOSED);
        assertThat(f.pagaEm()).isNull();
        assertThat(f.totalCongelado()).hasToString("1260.00");
    }

    @Test
    void transicoesForaDoCicloSaoRecusadas() {
        var hoje = LocalDate.of(2026, 11, 4);
        var paga = fechada().pagar(hoje, hoje);

        assertThatThrownBy(() -> aberta().pagar(hoje, hoje)).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> aberta().reabrir()).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> aberta().desfazerPagamento()).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> fechada().fechar(hoje, Dinheiro.ZERO)).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> fechada().desfazerPagamento()).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> paga.fechar(hoje, Dinheiro.ZERO)).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> paga.reabrir()).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> paga.pagar(hoje, hoje)).isInstanceOf(RegraDoCartao.class);
    }
}
