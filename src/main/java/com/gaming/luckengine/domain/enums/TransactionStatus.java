package com.gaming.luckengine.domain.enums;

/**
 * Represents the outcome of a reward transaction.
 */
public enum TransactionStatus {
    /**
     * User won a reward
     */
    WIN,
    
    /**
     * User did not win (Better luck next time)
     */
    LOSS,
    
    /**
     * Transaction is pending processing
     */
    PENDING,
    
    /**
     * Transaction failed due to error
     */
    FAILED,
    
    /**
     * Transaction was refunded
     */
    REFUNDED
}

