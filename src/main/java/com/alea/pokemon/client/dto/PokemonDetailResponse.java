package com.alea.pokemon.client.dto;

import com.alea.pokemon.model.Pokemon;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDetailResponse(
        int id,
        String name,
        int weight,
        int height,
        @JsonProperty("base_experience") Integer baseExperience) {

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
