package com.alea.pokemon.application.service;

import com.alea.pokemon.application.exception.RankingsNotReadyException;
import com.alea.pokemon.application.port.in.GetPokemonRankingsUseCase;
import com.alea.pokemon.application.port.in.LoadPokemonRankingsUseCase;
import com.alea.pokemon.application.port.out.PokemonSourcePort;
import com.alea.pokemon.application.port.out.RankingsStorePort;
import com.alea.pokemon.domain.Pokemon;
import com.alea.pokemon.domain.PokemonRankings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PokemonRankingsService implements LoadPokemonRankingsUseCase, GetPokemonRankingsUseCase {

    private final PokemonSourcePort pokemonSource;
    private final RankingsStorePort rankingsStore;

    @Override
    public void load() {
        log.info("Loading Pokémon to build the rankings...");
        List<Pokemon> allPokemon = pokemonSource.fetchAllPokemon();

        if (allPokemon.isEmpty()) {
            log.warn("No Pokémon were loaded, the rankings stay unavailable");
            return;
        }

        rankingsStore.save(PokemonRankings.from(allPokemon));
        log.info("Rankings are ready, built from {} Pokémon", allPokemon.size());
    }

    @Override
    public List<Pokemon> heaviest() {
        return currentRankings().heaviest();
    }

    @Override
    public List<Pokemon> highest() {
        return currentRankings().highest();
    }

    @Override
    public List<Pokemon> mostBaseExperience() {
        return currentRankings().mostBaseExperience();
    }

    @Override
    public boolean isReady() {
        return rankingsStore.find().isPresent();
    }

    private PokemonRankings currentRankings() {
        return rankingsStore.find().orElseThrow(RankingsNotReadyException::new);
    }
}
