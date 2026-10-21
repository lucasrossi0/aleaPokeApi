package com.alea.pokemon.adapter.in.startup;

import com.alea.pokemon.application.port.in.LoadPokemonRankingsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts loading the rankings as soon as the application is up.
 * The loading runs in its own virtual thread, so the web server is already answering
 * (with 503 Not Ready) while the Pokémon are being fetched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingsStartupTrigger {

    private final LoadPokemonRankingsUseCase loadRankings;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        Thread.ofVirtual().name("rankings-loader").start(this::loadSafely);
    }

    // If loading fails we only log it: the application keeps running and the endpoints keep answering 503.
    void loadSafely() {
        try {
            loadRankings.load();
        } catch (RuntimeException e) {
            log.error("The rankings could not be loaded", e);
        }
    }
}
