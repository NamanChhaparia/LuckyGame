package com.gaming.luckengine.exception;

/**
 * Exception thrown when there is insufficient voucher inventory.
 */
public class InsufficientInventoryException extends RuntimeException {
    
    public InsufficientInventoryException(String message) {
        super(message);
    }

    public InsufficientInventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}

