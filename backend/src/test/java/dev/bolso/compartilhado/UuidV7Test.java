package dev.bolso.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void temVersao7EVarianteDaRfc() {
        var id = UuidV7.gerar();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void guardaOMomentoNosPrimeiros48Bits() {
        var id = UuidV7.gerar(1_760_000_000_123L);

        assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(1_760_000_000_123L);
    }

    @Test
    void doisIdsNoMesmoMilissegundoSaoDiferentes() {
        var ids = new HashSet<UUID>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(UuidV7.gerar(1_760_000_000_000L));
        }

        assertThat(ids).hasSize(10_000);
    }

    @Test
    void idsDeMilissegundosDistintosFicamEmOrdem() {
        var antes = UuidV7.gerar(1_760_000_000_000L);
        var depois = UuidV7.gerar(1_760_000_000_001L);

        assertThat(antes.toString()).isLessThan(depois.toString());
    }
}
