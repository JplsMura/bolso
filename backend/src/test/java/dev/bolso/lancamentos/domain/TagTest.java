package dev.bolso.lancamentos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TagTest {

    static final UUID ESPACO = UUID.fromString("00000000-0000-7000-8000-0000000000c1");

    @Test
    void aparaONome() {
        assertThat(Tag.nova(UUID.randomUUID(), ESPACO, "  Luz  ", TipoCusto.FIXED).nome()).isEqualTo("Luz");
    }

    @Test
    void tipoDeCustoEOpcional() {
        assertThat(Tag.nova(UUID.randomUUID(), ESPACO, "Uber", null).tipoCusto()).isNull();
    }

    @Test
    void nomeDe1a40Caracteres() {
        assertThat(Tag.nova(UUID.randomUUID(), ESPACO, "a".repeat(40), null).nome()).hasSize(40);
        assertThatThrownBy(() -> Tag.nova(UUID.randomUUID(), ESPACO, "a".repeat(41), null)).isInstanceOf(RegraDeNegocio.class);
        assertThatThrownBy(() -> Tag.nova(UUID.randomUUID(), ESPACO, "  ", null)).isInstanceOf(RegraDeNegocio.class);
        assertThatThrownBy(() -> Tag.nova(UUID.randomUUID(), ESPACO, null, null)).isInstanceOf(RegraDeNegocio.class);
    }
}
