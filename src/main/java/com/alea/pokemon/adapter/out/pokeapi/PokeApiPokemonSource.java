package com.alea.pokemon.adapter.out.pokeapi;

import com.alea.pokemon.exception.PokemonSourceException;
import com.alea.pokemon.application.port.out.PokemonSourcePort;
import com.alea.pokemon.model.Pokemon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The PokemonSourcePort implementation that uses PokéAPI.
 * This is the door between PokéAPI and the rest of the application: what comes out are domain Pokémon.
 */
@Component
@RequiredArgsConstructor
public class PokeApiPokemonSource implements PokemonSourcePort {

    private final PokeApiClient pokeApiClient;
    private final ApiClientExecutor apiClientExecutor;

    @Override
    public List<Pokemon> fetchAllPokemon() {
        List<String> urls = listUrls();
        return apiClientExecutor.fetchAll(urls);
    }

    private List<String> listUrls() {
        try {
            return pokeApiClient.listPokemonUrls();
        } catch (RuntimeException e) {
            throw new PokemonSourceException("Could not get the list of Pokémon from PokéAPI", e);
        }
    }
}
