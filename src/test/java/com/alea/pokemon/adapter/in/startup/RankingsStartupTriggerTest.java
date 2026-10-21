package com.alea.pokemon.adapter.in.startup;

import com.alea.pokemon.application.exception.PokemonSourceException;
import com.alea.pokemon.application.port.in.LoadPokemonRankingsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RankingsStartupTriggerTest {

    @Mock
    private LoadPokemonRankingsUseCase loadRankings;

    @InjectMocks
    private RankingsStartupTrigger trigger;

    @Test
    void onApplicationReady_startsLoadingInTheBackground() {
        trigger.onApplicationReady();

        // the load runs in another thread, so we wait up to 2 seconds for it
        verify(loadRankings, timeout(2000)).load();
    }

    @Test
    void loadSafely_whenLoadingFails_doesNotThrow() {
        doThrow(new PokemonSourceException("PokéAPI is down", null)).when(loadRankings).load();

        assertThatCode(() -> trigger.loadSafely()).doesNotThrowAnyException();
        verify(loadRankings).load();
    }
}
