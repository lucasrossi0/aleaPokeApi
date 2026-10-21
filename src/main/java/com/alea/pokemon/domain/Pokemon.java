package com.alea.pokemon.domain;

import lombok.Builder;
import lombok.Value;

/**
 * A Pokémon as our application understands it.
 * It does not know where the data came from (PokéAPI, a database, a file...).
 */
@Value
@Builder
public class Pokemon {

    int id;
    String name;
    int weight;
    int height;
    int baseExperience;
}
