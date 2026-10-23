package com.alea.pokemon.client;

import com.alea.pokemon.client.dto.PokemonDetailResponse;
import com.alea.pokemon.client.dto.PokemonListItem;
import com.alea.pokemon.client.dto.PokemonListResponse;
import com.alea.pokemon.model.Pokemon;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

@Component
public class PokeApiClient {

    private static final int ALL_POKEMON_LIMIT = 100_000;

    private final RestClient restClient;

    public PokeApiClient(RestClient pokeApiRestClient) {
        this.restClient = pokeApiRestClient;
    }

    public List<String> listPokemonUrls() {
        PokemonListResponse response = restClient.get()
                .uri("/pokemon?limit={limit}", ALL_POKEMON_LIMIT)
                .retrieve()
                .body(PokemonListResponse.class);

        if (response == null) {
            throw new IllegalStateException("PokéAPI returned an empty body for the list of Pokémon");
        }

        return response.results().stream()
                .map(PokemonListItem::url)
                .toList();
    }

    public Pokemon fetchPokemon(String url) {
        PokemonDetailResponse response = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(PokemonDetailResponse.class);

        if (response == null) {
            throw new IllegalStateException("PokéAPI returned an empty body for " + url);
        }

        return response.toDomain();
    }
}
