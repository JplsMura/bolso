package dev.bolso;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Prova que as regras de ArquiteturaHexagonalTest pegam violações, já que hoje os pacotes
 * de produção estão vazios. As violações moram em src/test/java/fixtures (fora de dev.bolso,
 * para não entrar no ApplicationModules).
 */
class ArquiteturaHexagonalMordeTest {

    static final JavaClasses VIOLACOES = new ClassFileImporter().importPackages("fixtures.arquitetura");

    static Stream<ArchRule> regras() {
        return Stream.of(
                ArquiteturaHexagonalTest.dominioEhJavaPuro,
                ArquiteturaHexagonalTest.dominioNaoUsaJpa,
                ArquiteturaHexagonalTest.aplicacaoNaoConheceAdaptadores,
                ArquiteturaHexagonalTest.entradaNaoUsaSaida,
                ArquiteturaHexagonalTest.saidaNaoUsaEntrada);
    }

    @ParameterizedTest
    @MethodSource("regras")
    void regraPegaAViolacao(ArchRule regra) {
        assertThatThrownBy(() -> regra.check(VIOLACOES)).isInstanceOf(AssertionError.class);
    }
}
