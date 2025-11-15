package com.bloomberg.fxdeals.exception;

/**
 * Exception thrown when attempting to create a deal with a duplicate unique ID.
 */
public class DuplicateDealException extends RuntimeException {

    public DuplicateDealException(String message) {
        super(message);
    }

    public DuplicateDealException(String dealUniqueId, String message) {
        super(String.format("Deal with ID '%s' already exists: %s", dealUniqueId, message));
    }
}