package com.gaming.luckengine.exception;

/**
 * Exception thrown when a game state operation is invalid.
 */
public class GameStateException extends RuntimeException {
    
    public GameStateException(String message) {
        super(message);
    }

    public GameStateException(String message, Throwable cause) {
        super(message, cause);
    }
}

