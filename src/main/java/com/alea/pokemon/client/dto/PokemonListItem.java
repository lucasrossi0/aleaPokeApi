package com.alea.pokemon.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One entry of PokéAPI's list: the name and the address of the Pokémon's details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonListItem(String name, String url) {
}
