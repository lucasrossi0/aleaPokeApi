package com.alea.pokemon.client.dto;

import com.alea.pokemon.model.Pokemon;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Answer of GET /pokemon/{id}. We only declare the fields we need.
 * base_experience can be null in PokéAPI (some forms have no value), so it is an Integer here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDetailResponse(
        int id,
        String name,
        int weight,
        int height,
        @JsonProperty("base_experience") Integer baseExperience) {

    /**
     * Converts PokéAPI's shape into our own Pokemon model. A missing base experience counts as 0.
     */
    public Pokemon toDomain() {
        return Pokemon.builder()
                .id(id)
                .name(name)
                .weight(weight)
                .height(height)
                .baseExperience(baseExperience == null ? 0 : baseExperience)
                .build();
    }
}
