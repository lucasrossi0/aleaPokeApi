package com.alea.pokemon.adapter.in.startup;

import com.alea.pokemon.exception.PokemonSourceException;
import com.alea.pokemon.service.PokemonRankingsService;
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
    private PokemonRankingsService rankingsService;

    @InjectMocks
    private RankingsStartupTrigger trigger;

    @Test
    void onApplicationReady_startsLoadingInTheBackground() {
        trigger.onApplicationReady();

        // the load runs in another thread, so we wait up to 2 seconds for it
        verify(rankingsService, timeout(2000)).load();
    }

    @Test
    void loadSafely_whenLoadingFails_doesNotThrow() {
        doThrow(new PokemonSourceException("PokéAPI is down", null)).when(rankingsService).load();

        assertThatCode(() -> trigger.loadSafely()).doesNotThrowAnyException();
        verify(rankingsService).load();
    }
}
