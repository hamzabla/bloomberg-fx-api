package com.bloomberg.fxdeals.unit;

import com.bloomberg.fxdeals.exception.DealNotFoundException;
import com.bloomberg.fxdeals.exception.DuplicateDealException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for custom exception classes.
 * Tests all constructors to ensure 100% coverage.
 */
class ExceptionsTest {

    @Test
    void duplicateDealException_WithMessage_ShouldCreateException() {
        String message = "Test duplicate message";

        DuplicateDealException exception = new DuplicateDealException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void duplicateDealException_WithDealIdAndMessage_ShouldCreateException() {
        String dealId = "DEAL-001";
        String message = "already exists";

        DuplicateDealException exception = new DuplicateDealException(dealId, message);

        assertTrue(exception.getMessage().contains(dealId));
        assertTrue(exception.getMessage().contains(message));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void dealNotFoundException_WithMessage_ShouldCreateException() {
        String message = "Deal not found";

        DealNotFoundException exception = new DealNotFoundException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void dealNotFoundException_WithId_ShouldCreateException() {
        Long id = 123L;

        DealNotFoundException exception = new DealNotFoundException(id);

        assertTrue(exception.getMessage().contains(id.toString()));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void dealNotFoundException_WithUniqueId_ShouldCreateException() {
        String uniqueId = "DEAL-UNIQUE-001";

        DealNotFoundException exception = new DealNotFoundException(uniqueId, true);

        assertTrue(exception.getMessage().contains(uniqueId));
        assertTrue(exception.getMessage().contains("not found"));
    }
}