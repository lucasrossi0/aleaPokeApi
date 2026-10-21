package com.alea.pokemon.application.port.in;

/**
 * Use case: get all the Pokémon, build the rankings and keep them ready for requests.
 */
public interface LoadPokemonRankingsUseCase {

    void load();
}
