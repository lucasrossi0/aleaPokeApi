package com.alea.pokemon.adapter.in.web;

import com.alea.pokemon.application.port.in.GetPokemonRankingsUseCase;
import com.alea.pokemon.domain.Pokemon;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pokemon")
@RequiredArgsConstructor
public class PokemonController {

    private final GetPokemonRankingsUseCase getRankings;

    @GetMapping("/heaviest")
    public List<PokemonResponse> heaviest() {
        return toResponses(getRankings.heaviest());
    }

    @GetMapping("/highest")
    public List<PokemonResponse> highest() {
        return toResponses(getRankings.highest());
    }

    @GetMapping("/base-experience")
    public List<PokemonResponse> mostBaseExperience() {
        return toResponses(getRankings.mostBaseExperience());
    }

    private List<PokemonResponse> toResponses(List<Pokemon> pokemon) {
        return pokemon.stream()
                .map(PokemonResponse::from)
                .toList();
    }
}
