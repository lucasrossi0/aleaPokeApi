package com.alea.pokemon.application.port.out;

import com.alea.pokemon.model.PokemonRankings;

import java.util.Optional;

/**
 * Where the finished rankings are kept (today: in memory).
 */
public interface RankingsStorePort {

    void save(PokemonRankings rankings);

    /**
     * @return the rankings, or empty if they were never saved
     */
    Optional<PokemonRankings> find();
}
