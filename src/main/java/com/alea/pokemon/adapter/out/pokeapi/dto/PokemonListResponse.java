package com.alea.pokemon.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Answer of GET /pokemon?limit=...
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonListResponse(List<PokemonListItem> results) {
}
