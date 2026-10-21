package com.alea.pokemon.domain;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The three top-5 lists we expose.
 */
public record PokemonRankings(
        List<Pokemon> heaviest,
        List<Pokemon> highest,
        List<Pokemon> mostBaseExperience) {

    private static final int TOP_SIZE = 5;

    /**
     * Builds the three rankings from a list of Pokémon.
     */
    public static PokemonRankings from(List<Pokemon> pokemon) {
        return new PokemonRankings(
                topFive(pokemon, Pokemon::getWeight),
                topFive(pokemon, Pokemon::getHeight),
                topFive(pokemon, Pokemon::getBaseExperience));
    }

    // Biggest value first. If two Pokémon have the same value, the name (A-Z) decides,
    // so the result is always the same for the same input.
    private static List<Pokemon> topFive(List<Pokemon> pokemon, ToIntFunction<Pokemon> valueToRank) {
        Comparator<Pokemon> biggestFirst = Comparator
                .comparingInt(valueToRank)
                .reversed()
                .thenComparing(Pokemon::getName);

        return pokemon.stream()
                .sorted(biggestFirst)
                .limit(TOP_SIZE)
                .toList();
    }
}
