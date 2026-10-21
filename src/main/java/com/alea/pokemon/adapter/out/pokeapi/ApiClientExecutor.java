package com.alea.pokemon.adapter.out.pokeapi;

import com.alea.pokemon.domain.Pokemon;
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

/**
 * Calls PokéAPI for many Pokémon at the same time, so we don't wait for ~1350 calls one by one.
 * <p>
 * - Every Pokémon gets its own virtual thread (cheap threads, perfect for waiting on the network).
 * - A Semaphore limits how many calls run at once, to be kind to PokéAPI.
 * - A call that fails is tried again; if it keeps failing, that Pokémon is skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiClientExecutor {

    private final PokeApiClient pokeApiClient;
    private final PokeApiProperties properties;

    /**
     * @return the Pokémon that could be loaded (the ones that kept failing are left out)
     */
    public List<Pokemon> fetchAll(List<String> urls) {
        Semaphore allowedCalls = new Semaphore(properties.maxConcurrentCalls());

        // The executor is closed at the end of the try block, which waits for every task to finish.
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

    // Waits for a free slot, fetches the Pokémon, and gives the slot back.
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
            // fetchLimited never throws, but if it ever does we skip that Pokémon instead of failing everything
            log.warn("Unexpected error while loading a Pokémon", e.getCause());
            return Optional.empty();
        }
    }
}
