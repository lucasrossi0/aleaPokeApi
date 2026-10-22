package com.alea.pokemon.cache;

import com.alea.pokemon.model.PokemonRankings;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Keeps the finished rankings in memory, so requests never have to rebuild them.
 * AtomicReference makes it safe to save from the loading thread and read from request threads.
 */
@Component
public class RankingsCache {

    private final AtomicReference<PokemonRankings> rankings = new AtomicReference<>();

    public void save(PokemonRankings newRankings) {
        rankings.set(newRankings);
    }

    public Optional<PokemonRankings> find() {
        return Optional.ofNullable(rankings.get());
    }
}
