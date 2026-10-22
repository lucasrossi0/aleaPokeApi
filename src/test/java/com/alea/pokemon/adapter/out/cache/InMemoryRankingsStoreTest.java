package com.alea.pokemon.adapter.out.cache;

import com.alea.pokemon.model.PokemonRankings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRankingsStoreTest {

    private final InMemoryRankingsStore store = new InMemoryRankingsStore();

    @Test
    void find_beforeSaving_isEmpty() {
        assertThat(store.find()).isEmpty();
    }

    @Test
    void find_afterSaving_returnsWhatWasSaved() {
        PokemonRankings rankings = new PokemonRankings(List.of(), List.of(), List.of());

        store.save(rankings);

        assertThat(store.find()).containsSame(rankings);
    }

    @Test
    void save_twice_keepsTheLatest() {
        PokemonRankings first = new PokemonRankings(List.of(), List.of(), List.of());
        PokemonRankings second = new PokemonRankings(List.of(), List.of(), List.of());

        store.save(first);
        store.save(second);

        assertThat(store.find()).containsSame(second);
    }
}
