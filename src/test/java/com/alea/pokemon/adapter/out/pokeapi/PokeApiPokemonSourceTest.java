package com.alea.pokemon.adapter.out.pokeapi;

import com.alea.pokemon.exception.PokemonSourceException;
import com.alea.pokemon.model.Pokemon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokeApiPokemonSourceTest {

    @Mock
    private PokeApiClient pokeApiClient;

    @Mock
    private ApiClientExecutor apiClientExecutor;

    @InjectMocks
    private PokeApiPokemonSource source;

    @Test
    void fetchAllPokemon_getsTheUrlsAndFetchesThemAll() {
        List<String> urls = List.of("url-1", "url-2");
        Pokemon pikachu = Pokemon.builder().id(25).name("pikachu").weight(60).height(4).baseExperience(112).build();
        when(pokeApiClient.listPokemonUrls()).thenReturn(urls);
        when(apiClientExecutor.fetchAll(urls)).thenReturn(List.of(pikachu));

        assertThat(source.fetchAllPokemon()).containsExactly(pikachu);
    }

    @Test
    void fetchAllPokemon_whenTheListCallFails_throwsPokemonSourceException() {
        when(pokeApiClient.listPokemonUrls()).thenThrow(new RuntimeException("PokéAPI is down"));

        assertThatThrownBy(() -> source.fetchAllPokemon())
                .isInstanceOf(PokemonSourceException.class)
                .hasCauseInstanceOf(RuntimeException.class);
        verifyNoInteractions(apiClientExecutor);
    }
}
