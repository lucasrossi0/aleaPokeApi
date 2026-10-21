package com.alea.pokemon.application.port.in;

import com.alea.pokemon.domain.Pokemon;

import java.util.List;

/**
 * Use case: read the rankings that were already built.
 * Every "get" method throws RankingsNotReadyException while the rankings are not loaded yet.
 */
public interface GetPokemonRankingsUseCase {

    List<Pokemon> heaviest();

    List<Pokemon> highest();

    List<Pokemon> mostBaseExperience();

    boolean isReady();
}
