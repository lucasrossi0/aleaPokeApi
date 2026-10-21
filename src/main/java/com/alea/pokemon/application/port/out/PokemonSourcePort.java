package com.alea.pokemon.application.port.out;

import com.alea.pokemon.domain.Pokemon;

import java.util.List;

/**
 * Where the Pokémon come from. The application only says "give me all Pokémon";
 * an adapter decides how (today: PokéAPI).
 */
public interface PokemonSourcePort {

    /**
     * @throws com.alea.pokemon.application.exception.PokemonSourceException if the source cannot be used at all
     */
    List<Pokemon> fetchAllPokemon();
}
