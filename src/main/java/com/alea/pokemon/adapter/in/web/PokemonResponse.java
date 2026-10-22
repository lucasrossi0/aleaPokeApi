package com.alea.pokemon.adapter.in.web;

import com.alea.pokemon.model.Pokemon;

/**
 * The JSON we send to the caller. It is separate from the domain Pokemon,
 * so we can change the API format without touching the domain.
 */
public record PokemonResponse(int id, String name, int weight, int height, int baseExperience) {

    public static PokemonResponse from(Pokemon pokemon) {
        return new PokemonResponse(
                pokemon.getId(),
                pokemon.getName(),
                pokemon.getWeight(),
                pokemon.getHeight(),
                pokemon.getBaseExperience());
    }
}
