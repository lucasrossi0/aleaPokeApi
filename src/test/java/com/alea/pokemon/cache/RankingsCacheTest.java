package com.alea.pokemon.cache;

import com.alea.pokemon.model.PokemonRankings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankingsCacheTest {

    private final RankingsCache cache = new RankingsCache();

    @Test
    void find_beforeSaving_isEmpty() {
        assertThat(cache.find()).isEmpty();
    }

    @Test
    void find_afterSaving_returnsWhatWasSaved() {
        PokemonRankings rankings = new PokemonRankings(List.of(), List.of(), List.of());

        cache.save(rankings);

        assertThat(cache.find()).containsSame(rankings);
    }

    @Test
    void save_twice_keepsTheLatest() {
        PokemonRankings first = new PokemonRankings(List.of(), List.of(), List.of());
        PokemonRankings second = new PokemonRankings(List.of(), List.of(), List.of());

        cache.save(first);
        cache.save(second);

        assertThat(cache.find()).containsSame(second);
    }
}
