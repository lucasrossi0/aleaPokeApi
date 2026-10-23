package com.alea.pokemon.health;

import com.alea.pokemon.service.PokemonRankingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingsHealthIndicator implements HealthIndicator {

    private final PokemonRankingsService rankingsService;

    @Override
    public Health health() {
        if (rankingsService.isReady()) {
            return Health.up().build();
        }
        return Health.outOfService().withDetail("reason", "Rankings are still loading").build();
    }
}
