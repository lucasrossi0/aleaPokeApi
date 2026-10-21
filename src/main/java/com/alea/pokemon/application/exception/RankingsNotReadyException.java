package com.alea.pokemon.application.exception;

/**
 * Someone asked for the rankings before they were loaded.
 */
public class RankingsNotReadyException extends RuntimeException {

    public RankingsNotReadyException() {
        super("The rankings are still being loaded, please try again in a few seconds");
    }
}
