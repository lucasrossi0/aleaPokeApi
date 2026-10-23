package com.alea.pokemon;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;


@AnalyzeClasses(packages = "com.alea.pokemon", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layersFollowTheFlow = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("EntryPoints").definedBy("com.alea.pokemon.controller..", "com.alea.pokemon.health..", "com.alea.pokemon.startup..")
            .layer("Service").definedBy("com.alea.pokemon.service..")
            .layer("Client").definedBy("com.alea.pokemon.client..")
            .layer("Cache").definedBy("com.alea.pokemon.cache..")
            .whereLayer("EntryPoints").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("EntryPoints")
            .whereLayer("Client").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Cache").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule modelIsPlainJava = classes()
            .that().resideInAPackage("..model..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..model..", "java..", "lombok..");

    @ArchTest
    static final ArchRule serviceDoesNotKnowTheWeb = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "jakarta.servlet..");

    @ArchTest
    static final ArchRule onlyTheClientPackageKnowsPokeApi = noClasses()
            .that().resideOutsideOfPackage("..client..")
            .should().dependOnClassesThat(resideInAPackage("..client..").and(not(simpleName("PokemonLoader"))));

    @ArchTest
    static final ArchRule noCyclesBetweenPackages = slices()
            .matching("com.alea.pokemon.(*)..")
            .should().beFreeOfCycles();
}
