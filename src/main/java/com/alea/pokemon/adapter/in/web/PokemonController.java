package com.alea.pokemon.adapter.in.web;

import com.alea.pokemon.service.PokemonRankingsService;
import com.alea.pokemon.model.Pokemon;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pokemon")
@RequiredArgsConstructor
public class PokemonController {

    private final PokemonRankingsService rankingsService;

    @GetMapping("/heaviest")
    public List<PokemonResponse> heaviest() {
        return toResponses(rankingsService.heaviest());
    }

    @GetMapping("/highest")
    public List<PokemonResponse> highest() {
        return toResponses(rankingsService.highest());
    }

    @GetMapping("/base-experience")
    public List<PokemonResponse> mostBaseExperience() {
        return toResponses(rankingsService.mostBaseExperience());
    }

    private List<PokemonResponse> toResponses(List<Pokemon> pokemon) {
        return pokemon.stream()
                .map(PokemonResponse::from)
                .toList();
    }
}
