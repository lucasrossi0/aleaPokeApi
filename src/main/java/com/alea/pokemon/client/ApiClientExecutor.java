package com.alea.pokemon.client;

import com.alea.pokemon.exception.PokemonSourceException;
import com.alea.pokemon.model.Pokemon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiClientExecutor {

    private final PokeApiClient pokeApiClient;
    private final PokeApiProperties properties;

    public List<Pokemon> fetchAllPokemon() {
        return fetchAll(listUrls());
    }

    private List<String> listUrls() {
        try {
            return pokeApiClient.listPokemonUrls();
        } catch (RuntimeException e) {
            throw new PokemonSourceException("Could not get the list of Pokémon from PokéAPI", e);
        }
    }

    private List<Pokemon> fetchAll(List<String> urls) {
        Semaphore allowedCalls = new Semaphore(properties.maxConcurrentCalls());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Optional<Pokemon>>> futures = urls.stream()
                    .map(url -> executor.submit(() -> fetchLimited(url, allowedCalls)))
                    .toList();

            return futures.stream()
                    .map(this::resultOf)
                    .flatMap(Optional::stream)
                    .toList();
        }
    }

    private Optional<Pokemon> fetchLimited(String url, Semaphore allowedCalls) {
        try {
            allowedCalls.acquire();
            try {
                return fetchWithRetries(url);
            } finally {
                allowedCalls.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<Pokemon> fetchWithRetries(String url) throws InterruptedException {
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                return Optional.of(pokeApiClient.fetchPokemon(url));
            } catch (RuntimeException e) {
                log.debug("Attempt {} of {} failed for {}: {}", attempt, properties.maxAttempts(), url, e.getMessage());
                if (attempt < properties.maxAttempts()) {
                    Thread.sleep(properties.retryDelay());
                }
            }
        }
        log.warn("Skipping {} after {} failed attempts", url, properties.maxAttempts());
        return Optional.empty();
    }

    private Optional<Pokemon> resultOf(Future<Optional<Pokemon>> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            log.warn("Unexpected error while loading a Pokémon", e.getCause());
            return Optional.empty();
        }
    }
}
