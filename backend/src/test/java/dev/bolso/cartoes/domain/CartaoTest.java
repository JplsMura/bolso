package dev.bolso.cartoes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.compartilhado.Dinheiro;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartaoTest {

    static final UUID ID = UUID.fromString("00000000-0000-7000-8000-0000000000d1");
    static final UUID ESPACO = UUID.fromString("00000000-0000-7000-8000-0000000000c1");

    static DadosDoCartao dados(String nome, int fecha, int vence, String limite) {
        return new DadosDoCartao(nome, fecha, vence, Dinheiro.deTexto(limite));
    }

    @Test
    void novoAparaONomeENasceNaVersaoZero() {
        var c = Cartao.novo(ID, ESPACO, dados("  Cartão A  ", 25, 5, "4500.00"));

        assertThat(c.nome()).isEqualTo("Cartão A");
        assertThat(c.versao()).isZero();
        assertThat(c.limite()).hasToString("4500.00");
    }

    @Test
    void nomeDe1A40() {
        assertThatThrownBy(() -> Cartao.novo(ID, ESPACO, dados("  ", 25, 5, "1.00"))).isInstanceOf(RegraDoCartao.class);
        assertThatThrownBy(() -> Cartao.novo(ID, ESPACO, dados("x".repeat(41), 25, 5, "1.00"))).isInstanceOf(RegraDoCartao.class);
        assertThat(Cartao.novo(ID, ESPACO, dados("x".repeat(40), 25, 5, "1.00")).nome()).hasSize(40);
    }

    @Test
    void limiteMaiorQueZero() {
        assertThatThrownBy(() -> Cartao.novo(ID, ESPACO, dados("A", 25, 5, "0.00"))).hasMessageContaining("limite");
    }

    @Test
    void diasForaDaFaixa() {
        assertThatThrownBy(() -> Cartao.novo(ID, ESPACO, dados("A", 32, 5, "1.00"))).isInstanceOf(RegraDoCartao.class);
    }

    @Test
    void editarMantemIdEVersao() {
        var c = Cartao.reconstituir(ID, ESPACO, dados("A", 25, 5, "1000.00"), 3);

        var editado = c.editar(dados("B", 10, 20, "2000.00"));

        assertThat(editado.id()).isEqualTo(ID);
        assertThat(editado.versao()).isEqualTo(3);
        assertThat(editado.diaFechamento()).isEqualTo(10);
    }
}
