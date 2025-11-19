package com.gaming.luckengine.domain.enums;

/**
 * Represents the lifecycle status of a Game.
 */
public enum GameStatus {
    /**
     * Game is scheduled but not yet started
     */
    SCHEDULED,
    
    /**
     * Game is currently active and accepting players
     */
    ACTIVE,
    
    /**
     * Game has completed normally
     */
    COMPLETED,
    
    /**
     * Game was cancelled before or during execution
     */
    CANCELLED,
    
    /**
     * Game budget has been exhausted
     */
    BUDGET_EXHAUSTED
}

