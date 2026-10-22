package com.alea.pokemon;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * These tests protect the hexagonal architecture: if someone breaks a rule, the build fails.
 * <p>
 * The big picture: adapter -> application -> domain (arrows = "is allowed to use").
 */
@AnalyzeClasses(packages = "com.alea.pokemon", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layersOnlyPointInward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("com.alea.pokemon.model..")
            .layer("Application").definedBy("com.alea.pokemon.application..")
            .layer("Adapter").definedBy("com.alea.pokemon.adapter..")
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter");

    // "lombok.." is allowed because Lombok only marks the code it generates; it is not a runtime dependency.
    @ArchTest
    static final ArchRule domainIsPlainJava = classes()
            .that().resideInAPackage("..model..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..model..", "java..", "lombok..");

    @ArchTest
    static final ArchRule applicationOnlyKnowsTheDomain = classes()
            .that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "..application..", "..model..", "..exception..", "java..", "lombok..", "org.slf4j..", "org.springframework.stereotype..");

    @ArchTest
    static final ArchRule onlyThePokeApiAdapterKnowsPokeApi = noClasses()
            .that().resideOutsideOfPackage("..adapter.out.pokeapi..")
            .should().dependOnClassesThat(resideInAPackage("..adapter.out.pokeapi.."));

    @ArchTest
    static final ArchRule adaptersDoNotKnowEachOther = slices()
            .matching("..adapter.(*).(*)..")
            .should().notDependOnEachOther();

    @ArchTest
    static final ArchRule noCyclesBetweenLayers = slices()
            .matching("com.alea.pokemon.(*)..")
            .should().beFreeOfCycles();
}
