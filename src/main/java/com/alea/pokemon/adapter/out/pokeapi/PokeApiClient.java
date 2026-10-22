package com.alea.pokemon.adapter.out.pokeapi;

import com.alea.pokemon.adapter.out.pokeapi.dto.PokemonDetailResponse;
import com.alea.pokemon.adapter.out.pokeapi.dto.PokemonListItem;
import com.alea.pokemon.adapter.out.pokeapi.dto.PokemonListResponse;
import com.alea.pokemon.model.Pokemon;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

/**
 * Makes the HTTP calls to PokéAPI. One method = one call. No retries here (see ApiClientExecutor).
 * Any HTTP or parsing problem is thrown as a RuntimeException.
 */
@Component
public class PokeApiClient {

    // PokéAPI has about 1350 Pokémon, so this number is bigger than all of them: we get the whole list in one call.
    private static final int ALL_POKEMON_LIMIT = 100_000;

    private final RestClient restClient;

    public PokeApiClient(RestClient pokeApiRestClient) {
        this.restClient = pokeApiRestClient;
    }

    /**
     * @return the address of the details of every Pokémon
     */
    public List<String> listPokemonUrls() {
        PokemonListResponse response = restClient.get()
                .uri("/pokemon?limit={limit}", ALL_POKEMON_LIMIT)
                .retrieve()
                .body(PokemonListResponse.class);

        return response.results().stream()
                .map(PokemonListItem::url)
                .toList();
    }

    /**
     * @param url address taken from {@link #listPokemonUrls()}
     */
    public Pokemon fetchPokemon(String url) {
        PokemonDetailResponse response = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(PokemonDetailResponse.class);

        return response.toDomain();
    }
}
