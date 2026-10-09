package dev.bolso;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Regras hexagonais dentro de cada módulo. allowEmptyShould: os pacotes nascem vazios
 * e as regras passam a morder conforme as features criam classes.
 */
@AnalyzeClasses(packages = "dev.bolso", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaHexagonalTest {

    @ArchTest
    static final ArchRule dominioEhJavaPuro = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "..application..", "..adapter..")
            .because("o domínio não conhece Spring, casos de uso nem adaptadores")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule dominioNaoUsaJpa = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .because("a entidade JPA vive em adapter.out.persistence; o domínio fica testável sem Spring nem Hibernate")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule aplicacaoNaoConheceAdaptadores = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .because("casos de uso falam com o mundo só por portas")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entradaNaoUsaSaida = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .because("controller chama caso de uso, não repositório")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule saidaNaoUsaEntrada = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in..")
            .allowEmptyShould(true);
}
