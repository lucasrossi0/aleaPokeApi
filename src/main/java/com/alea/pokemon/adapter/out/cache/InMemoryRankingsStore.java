package com.alea.pokemon.adapter.out.cache;

import com.alea.pokemon.application.port.out.RankingsStorePort;
import com.alea.pokemon.model.PokemonRankings;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Keeps the rankings in memory.
 * AtomicReference makes it safe to save from the loading thread and read from request threads.
 */
@Component
public class InMemoryRankingsStore implements RankingsStorePort {

    private final AtomicReference<PokemonRankings> rankings = new AtomicReference<>();

    @Override
    public void save(PokemonRankings newRankings) {
        rankings.set(newRankings);
    }

    @Override
    public Optional<PokemonRankings> find() {
        return Optional.ofNullable(rankings.get());
    }
}
