package com.alea.pokemon.service;

import com.alea.pokemon.exception.RankingsNotReadyException;
import com.alea.pokemon.cache.RankingsCache;
import com.alea.pokemon.client.ApiClientExecutor;
import com.alea.pokemon.model.Pokemon;
import com.alea.pokemon.model.PokemonRankings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PokemonRankingsService {

    private final ApiClientExecutor apiClientExecutor;
    private final RankingsCache rankingsCache;

    public void load() {
        log.info("Loading Pokémon to build the rankings...");
        List<Pokemon> allPokemon = apiClientExecutor.fetchAllPokemon();

        if (allPokemon.isEmpty()) {
            log.warn("No Pokémon were loaded, the rankings stay unavailable");
            return;
        }

        rankingsCache.save(PokemonRankings.from(allPokemon));
        log.info("Rankings are ready, built from {} Pokémon", allPokemon.size());
    }

    public List<Pokemon> heaviest() {
        return currentRankings().heaviest();
    }

    public List<Pokemon> highest() {
        return currentRankings().highest();
    }

    public List<Pokemon> mostBaseExperience() {
        return currentRankings().mostBaseExperience();
    }

    public boolean isReady() {
        return rankingsCache.find().isPresent();
    }

    private PokemonRankings currentRankings() {
        return rankingsCache.find().orElseThrow(RankingsNotReadyException::new);
    }
}
