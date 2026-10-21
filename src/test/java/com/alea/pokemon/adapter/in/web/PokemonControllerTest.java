package com.alea.pokemon.adapter.in.web;

import com.alea.pokemon.application.exception.RankingsNotReadyException;
import com.alea.pokemon.application.port.in.GetPokemonRankingsUseCase;
import com.alea.pokemon.domain.Pokemon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PokemonController.class)
class PokemonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPokemonRankingsUseCase getRankings;

    private final Pokemon snorlax = Pokemon.builder()
            .id(143).name("snorlax").weight(4600).height(21).baseExperience(189).build();
    private final Pokemon pikachu = Pokemon.builder()
            .id(25).name("pikachu").weight(60).height(4).baseExperience(112).build();

    @Test
    void heaviest_returnsTheListAsJson() throws Exception {
        when(getRankings.heaviest()).thenReturn(List.of(snorlax, pikachu));

        mockMvc.perform(get("/api/pokemon/heaviest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(143))
                .andExpect(jsonPath("$[0].name").value("snorlax"))
                .andExpect(jsonPath("$[0].weight").value(4600))
                .andExpect(jsonPath("$[0].height").value(21))
                .andExpect(jsonPath("$[0].baseExperience").value(189))
                .andExpect(jsonPath("$[1].name").value("pikachu"));
    }

    @Test
    void highest_returnsTheListAsJson() throws Exception {
        when(getRankings.highest()).thenReturn(List.of(snorlax));

        mockMvc.perform(get("/api/pokemon/highest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("snorlax"));
    }

    @Test
    void baseExperience_returnsTheListAsJson() throws Exception {
        when(getRankings.mostBaseExperience()).thenReturn(List.of(pikachu));

        mockMvc.perform(get("/api/pokemon/base-experience"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("pikachu"));
    }

    @Test
    void whenRankingsAreNotReady_answers503() throws Exception {
        when(getRankings.heaviest()).thenThrow(new RankingsNotReadyException());
        when(getRankings.highest()).thenThrow(new RankingsNotReadyException());
        when(getRankings.mostBaseExperience()).thenThrow(new RankingsNotReadyException());

        for (String path : List.of("heaviest", "highest", "base-experience")) {
            mockMvc.perform(get("/api/pokemon/" + path))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503))
                    .andExpect(jsonPath("$.title").value("Not Ready"))
                    .andExpect(jsonPath("$.detail").isNotEmpty());
        }
    }

    @Test
    void whenSomethingUnexpectedHappens_answers500WithoutDetails() throws Exception {
        when(getRankings.heaviest()).thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(get("/api/pokemon/heaviest"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("Something went wrong on our side"));
    }

    @Test
    void unknownPath_answers404() throws Exception {
        mockMvc.perform(get("/api/pokemon/unknown"))
                .andExpect(status().isNotFound());
    }
}
