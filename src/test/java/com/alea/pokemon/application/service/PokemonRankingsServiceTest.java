package com.alea.pokemon.application.service;

import com.alea.pokemon.exception.PokemonSourceException;
import com.alea.pokemon.exception.RankingsNotReadyException;
import com.alea.pokemon.application.port.out.PokemonSourcePort;
import com.alea.pokemon.application.port.out.RankingsStorePort;
import com.alea.pokemon.model.Pokemon;
import com.alea.pokemon.model.PokemonRankings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonRankingsServiceTest {

    @Mock
    private PokemonSourcePort pokemonSource;

    @Mock
    private RankingsStorePort rankingsStore;

    @InjectMocks
    private PokemonRankingsService service;

    private final Pokemon snorlax = pokemon("snorlax", 4600, 21, 189);
    private final Pokemon pikachu = pokemon("pikachu", 60, 4, 112);

    @Test
    void load_buildsRankingsFromTheSourceAndSavesThem() {
        when(pokemonSource.fetchAllPokemon()).thenReturn(List.of(snorlax, pikachu));

        service.load();

        ArgumentCaptor<PokemonRankings> saved = ArgumentCaptor.forClass(PokemonRankings.class);
        verify(rankingsStore).save(saved.capture());
        assertThat(saved.getValue().heaviest()).containsExactly(snorlax, pikachu);
    }

    @Test
    void load_withNoPokemon_savesNothing() {
        when(pokemonSource.fetchAllPokemon()).thenReturn(List.of());

        service.load();

        verify(rankingsStore, never()).save(any());
    }

    @Test
    void load_whenSourceFails_theExceptionGoesUp() {
        when(pokemonSource.fetchAllPokemon()).thenThrow(new PokemonSourceException("boom", null));

        assertThatThrownBy(() -> service.load()).isInstanceOf(PokemonSourceException.class);
        verify(rankingsStore, never()).save(any());
    }

    @Test
    void getters_returnTheStoredLists() {
        PokemonRankings rankings = new PokemonRankings(List.of(snorlax), List.of(pikachu), List.of(snorlax, pikachu));
        when(rankingsStore.find()).thenReturn(Optional.of(rankings));

        assertThat(service.heaviest()).containsExactly(snorlax);
        assertThat(service.highest()).containsExactly(pikachu);
        assertThat(service.mostBaseExperience()).containsExactly(snorlax, pikachu);
    }

    @Test
    void getters_beforeLoading_throwNotReady() {
        when(rankingsStore.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.heaviest()).isInstanceOf(RankingsNotReadyException.class);
        assertThatThrownBy(() -> service.highest()).isInstanceOf(RankingsNotReadyException.class);
        assertThatThrownBy(() -> service.mostBaseExperience()).isInstanceOf(RankingsNotReadyException.class);
    }

    @Test
    void isReady_isTrueOnlyWhenRankingsAreStored() {
        when(rankingsStore.find())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new PokemonRankings(List.of(), List.of(), List.of())));

        assertThat(service.isReady()).isFalse();
        assertThat(service.isReady()).isTrue();
    }

    private static Pokemon pokemon(String name, int weight, int height, int baseExperience) {
        return Pokemon.builder()
                .id(1)
                .name(name)
                .weight(weight)
                .height(height)
                .baseExperience(baseExperience)
                .build();
    }
}
