package com.alea.pokemon.adapter.in.health;

import com.alea.pokemon.application.port.in.GetPokemonRankingsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Shows in /actuator/health whether the rankings are loaded.
 * It is part of the "readiness" group (see application.yml): until it is UP, the app is not ready for traffic.
 * Spring names this indicator "rankings" (the class name without "HealthIndicator"), as used in application.yml.
 */
@Component
@RequiredArgsConstructor
public class RankingsHealthIndicator implements HealthIndicator {

    private final GetPokemonRankingsUseCase getRankings;

    @Override
    public Health health() {
        if (getRankings.isReady()) {
            return Health.up().build();
        }
        return Health.outOfService().withDetail("reason", "Rankings are still loading").build();
    }
}
