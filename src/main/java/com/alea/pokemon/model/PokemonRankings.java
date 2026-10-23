package com.alea.pokemon.model;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

public record PokemonRankings(
        List<Pokemon> heaviest,
        List<Pokemon> highest,
        List<Pokemon> mostBaseExperience) {

    private static final int TOP_SIZE = 5;

    public static PokemonRankings from(List<Pokemon> pokemon) {
        return new PokemonRankings(
                topFive(pokemon, Pokemon::getWeight),
                topFive(pokemon, Pokemon::getHeight),
                topFive(pokemon, Pokemon::getBaseExperience));
    }

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
