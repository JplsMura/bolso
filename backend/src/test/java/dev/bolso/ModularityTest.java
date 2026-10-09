package dev.bolso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fronteiras entre módulos: um módulo só enxerga a raiz pública do outro. */
class ModularityTest {

    static final ApplicationModules MODULES = ApplicationModules.of(BolsoApplication.class);

    @Test
    void detectaOsSeisModulos() {
        assertThat(MODULES.stream().map(m -> m.getBasePackage().getName()))
                .containsExactlyInAnyOrder(
                        "dev.bolso.identidade",
                        "dev.bolso.lancamentos",
                        "dev.bolso.cartoes",
                        "dev.bolso.orcamento",
                        "dev.bolso.faturamento",
                        "dev.bolso.compartilhado");
    }

    @Test
    void respeitaAsFronteiras() {
        MODULES.verify();
    }
}
