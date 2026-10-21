package com.alea.pokemon.adapter.out.pokeapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings for talking to PokéAPI. The values live in application.yml (prefix "pokeapi").
 *
 * @param baseUrl            address of PokéAPI, for example https://pokeapi.co/api/v2
 * @param connectTimeout     how long to wait to open a connection
 * @param readTimeout        how long to wait for the answer
 * @param maxAttempts        how many times each Pokémon call is tried before giving up on it
 * @param retryDelay         pause between two attempts of the same call
 * @param maxConcurrentCalls how many calls can be running at the same time
 */
@ConfigurationProperties(prefix = "pokeapi")
public record PokeApiProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        Duration retryDelay,
        int maxConcurrentCalls) {
}
