package com.alea.pokemon.model;

import lombok.Builder;
import lombok.Value;


@Value
@Builder
public class Pokemon {

    int id;
    String name;
    int weight;
    int height;
    int baseExperience;
}
