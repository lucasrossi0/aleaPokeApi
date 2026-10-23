package com.alea.pokemon.controller;

import com.alea.pokemon.model.Pokemon;

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
