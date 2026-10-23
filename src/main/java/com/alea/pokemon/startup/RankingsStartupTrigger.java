package com.alea.pokemon.startup;

import com.alea.pokemon.service.PokemonRankingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class RankingsStartupTrigger {

    private final PokemonRankingsService rankingsService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        Thread.ofVirtual().name("rankings-loader").start(this::loadSafely);
    }

    void loadSafely() {
        try {
            rankingsService.load();
        } catch (RuntimeException e) {
            log.error("The rankings could not be loaded", e);
        }
    }
}
