package com.alea.pokemon.exception;


public class RankingsNotReadyException extends RuntimeException {

    public RankingsNotReadyException() {
        super("The rankings are still being loaded, please try again in a few seconds");
    }
}
