package com.alea.pokemon.adapter.in.health;

import com.alea.pokemon.service.PokemonRankingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingsHealthIndicatorTest {

    @Mock
    private PokemonRankingsService rankingsService;

    @InjectMocks
    private RankingsHealthIndicator indicator;

    @Test
    void health_whenRankingsAreLoaded_isUp() {
        when(rankingsService.isReady()).thenReturn(true);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void health_whenRankingsAreStillLoading_isOutOfService() {
        when(rankingsService.isReady()).thenReturn(false);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsKey("reason");
    }
}
