package com.alea.pokemon.application.exception;

/**
 * The place we get Pokémon from could not be used.
 */
public class PokemonSourceException extends RuntimeException {

    public PokemonSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
