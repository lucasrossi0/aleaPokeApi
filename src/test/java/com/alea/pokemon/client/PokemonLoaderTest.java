package com.alea.pokemon.client;

import com.alea.pokemon.exception.PokemonSourceException;
import com.alea.pokemon.model.Pokemon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonLoaderTest {

    @Mock
    private PokeApiClient pokeApiClient;

    @Test
    void fetchAll_returnsEveryPokemon() {
        when(pokeApiClient.fetchPokemon("url-1")).thenReturn(pokemon("one"));
        when(pokeApiClient.fetchPokemon("url-2")).thenReturn(pokemon("two"));

        List<Pokemon> result = fetchAll(loader(3, 10), List.of("url-1", "url-2"));

        assertThat(result).extracting(Pokemon::getName).containsExactlyInAnyOrder("one", "two");
    }

    @Test
    void fetchAll_withNoUrls_returnsEmptyList() {
        assertThat(fetchAll(loader(3, 10), List.of())).isEmpty();
    }

    @Test
    void fetchAll_retriesAFailedCall_andKeepsThePokemonIfALaterAttemptWorks() {
        when(pokeApiClient.fetchPokemon("url-1"))
                .thenThrow(new RuntimeException("first attempt fails"))
                .thenThrow(new RuntimeException("second attempt fails"))
                .thenReturn(pokemon("one"));

        List<Pokemon> result = fetchAll(loader(3, 10), List.of("url-1"));

        assertThat(result).extracting(Pokemon::getName).containsExactly("one");
        verify(pokeApiClient, times(3)).fetchPokemon("url-1");
    }

    @Test
    void fetchAll_skipsAPokemonThatFailsEveryAttempt_andKeepsTheOthers() {
        when(pokeApiClient.fetchPokemon("bad-url")).thenThrow(new RuntimeException("always fails"));
        when(pokeApiClient.fetchPokemon("good-url")).thenReturn(pokemon("good"));

        List<Pokemon> result = fetchAll(loader(3, 10), List.of("bad-url", "good-url"));

        assertThat(result).extracting(Pokemon::getName).containsExactly("good");
        verify(pokeApiClient, times(3)).fetchPokemon("bad-url");
    }

    @Test
    void fetchAll_neverRunsMoreCallsAtOnceThanAllowed() {
        int maxConcurrentCalls = 3;
        AtomicInteger runningNow = new AtomicInteger();
        AtomicInteger mostAtOnce = new AtomicInteger();

        when(pokeApiClient.fetchPokemon(anyString())).thenAnswer(invocation -> {
            int running = runningNow.incrementAndGet();
            mostAtOnce.accumulateAndGet(running, Math::max);
            Thread.sleep(20);
            runningNow.decrementAndGet();
            return pokemon("any");
        });
        List<String> urls = IntStream.range(0, 30).mapToObj(i -> "url-" + i).toList();

        List<Pokemon> result = fetchAll(loader(1, maxConcurrentCalls), urls);

        assertThat(result).hasSize(30);
        assertThat(mostAtOnce.get()).isBetween(1, maxConcurrentCalls);
    }

    @Test
    void fetchAll_whenInterrupted_skipsThePokemon() {
        lenient().when(pokeApiClient.fetchPokemon("url-1")).thenAnswer(invocation -> {
            Thread.sleep(300);
            return pokemon("one");
        });

        Thread.currentThread().interrupt();
        List<Pokemon> result = fetchAll(loader(2, 10), List.of("url-1"));
        boolean wasInterrupted = Thread.interrupted();

        assertThat(result).isEmpty();
        assertThat(wasInterrupted).isTrue();
    }

    @Test
    void fetchAllPokemon_whenTheListCallFails_throwsPokemonSourceException() {
        when(pokeApiClient.listPokemonUrls()).thenThrow(new RuntimeException("PokéAPI is down"));

        assertThatThrownBy(() -> loader(3, 10).fetchAllPokemon())
                .isInstanceOf(PokemonSourceException.class)
                .hasCauseInstanceOf(RuntimeException.class);
        verify(pokeApiClient, never()).fetchPokemon(anyString());
    }

    private List<Pokemon> fetchAll(PokemonLoader loader, List<String> urls) {
        when(pokeApiClient.listPokemonUrls()).thenReturn(urls);
        return loader.fetchAllPokemon();
    }

    private PokemonLoader loader(int maxAttempts, int maxConcurrentCalls) {
        PokeApiProperties properties = new PokeApiProperties(
                "http://not-used", Duration.ofSeconds(1), Duration.ofSeconds(1),
                maxAttempts, Duration.ZERO, maxConcurrentCalls);
        return new PokemonLoader(pokeApiClient, properties);
    }

    private static Pokemon pokemon(String name) {
        return Pokemon.builder().id(1).name(name).weight(1).height(1).baseExperience(1).build();
    }
}
