package com.alea.pokemon.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pokeapi")
public record PokeApiProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        Duration retryDelay,
        int maxConcurrentCalls) {
}
