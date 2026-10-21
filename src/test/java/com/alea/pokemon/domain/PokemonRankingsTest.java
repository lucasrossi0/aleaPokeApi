package com.alea.pokemon.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PokemonRankingsTest {

    @Test
    void heaviest_returnsTheFiveWithMostWeight_biggestFirst() {
        List<Pokemon> pokemon = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> pokemon(i, "p" + i, i * 10, 1, 1))
                .toList();

        PokemonRankings rankings = PokemonRankings.from(pokemon);

        assertThat(rankings.heaviest()).extracting(Pokemon::getName)
                .containsExactly("p8", "p7", "p6", "p5", "p4");
    }

    @Test
    void highest_returnsTheFiveWithMostHeight_biggestFirst() {
        List<Pokemon> pokemon = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> pokemon(i, "p" + i, 1, i * 10, 1))
                .toList();

        PokemonRankings rankings = PokemonRankings.from(pokemon);

        assertThat(rankings.highest()).extracting(Pokemon::getName)
                .containsExactly("p8", "p7", "p6", "p5", "p4");
    }

    @Test
    void mostBaseExperience_returnsTheFiveWithMostExperience_biggestFirst() {
        List<Pokemon> pokemon = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> pokemon(i, "p" + i, 1, 1, i * 10))
                .toList();

        PokemonRankings rankings = PokemonRankings.from(pokemon);

        assertThat(rankings.mostBaseExperience()).extracting(Pokemon::getName)
                .containsExactly("p8", "p7", "p6", "p5", "p4");
    }

    @Test
    void sameValue_isOrderedByNameAscending() {
        List<Pokemon> pokemon = List.of(
                pokemon(1, "charmander", 100, 1, 1),
                pokemon(2, "abra", 100, 1, 1),
                pokemon(3, "bulbasaur", 100, 1, 1));

        PokemonRankings rankings = PokemonRankings.from(pokemon);

        assertThat(rankings.heaviest()).extracting(Pokemon::getName)
                .containsExactly("abra", "bulbasaur", "charmander");
    }

    @Test
    void fewerThanFivePokemon_returnsAllOfThem() {
        PokemonRankings rankings = PokemonRankings.from(List.of(pokemon(1, "pikachu", 60, 4, 112)));

        assertThat(rankings.heaviest()).hasSize(1);
        assertThat(rankings.highest()).hasSize(1);
        assertThat(rankings.mostBaseExperience()).hasSize(1);
    }

    @Test
    void emptyList_returnsEmptyRankings() {
        PokemonRankings rankings = PokemonRankings.from(List.of());

        assertThat(rankings.heaviest()).isEmpty();
        assertThat(rankings.highest()).isEmpty();
        assertThat(rankings.mostBaseExperience()).isEmpty();
    }

    private static Pokemon pokemon(int id, String name, int weight, int height, int baseExperience) {
        return Pokemon.builder()
                .id(id)
                .name(name)
                .weight(weight)
                .height(height)
                .baseExperience(baseExperience)
                .build();
    }
}
