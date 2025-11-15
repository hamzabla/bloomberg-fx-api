package com.bloomberg.fxdeals.exception;

/**
 * Exception thrown when a deal cannot be found.
 */
public class DealNotFoundException extends RuntimeException {

    public DealNotFoundException(String message) {
        super(message);
    }

    public DealNotFoundException(Long id) {
        super(String.format("Deal with ID %d not found", id));
    }

    public DealNotFoundException(String dealUniqueId, boolean byUniqueId) {
        super(String.format("Deal with unique ID '%s' not found", dealUniqueId));
    }
}