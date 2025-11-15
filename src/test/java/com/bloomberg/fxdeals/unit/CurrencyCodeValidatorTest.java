package com.bloomberg.fxdeals.unit;

import com.bloomberg.fxdeals.validation.CurrencyCodeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CurrencyCodeValidator.
 */
class CurrencyCodeValidatorTest {

    private CurrencyCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CurrencyCodeValidator();
    }

    @Test
    void isValid_WithValidCurrencyCode_ShouldReturnTrue() {
        assertTrue(validator.isValid("USD", null));
        assertTrue(validator.isValid("EUR", null));
        assertTrue(validator.isValid("GBP", null));
        assertTrue(validator.isValid("JPY", null));
        assertTrue(validator.isValid("CHF", null));
    }

    @Test
    void isValid_WithLowercaseValidCode_ShouldReturnTrue() {
        assertTrue(validator.isValid("usd", null));
        assertTrue(validator.isValid("eur", null));
    }

    @Test
    void isValid_WithMixedCaseValidCode_ShouldReturnTrue() {
        assertTrue(validator.isValid("UsD", null));
        assertTrue(validator.isValid("eUr", null));
    }

    @Test
    void isValid_WithInvalidCurrencyCode_ShouldReturnFalse() {
        assertFalse(validator.isValid("INVALID", null));
        assertFalse(validator.isValid("ABC", null));
        assertFalse(validator.isValid("123", null));
        assertFalse(validator.isValid("XYZ", null));
    }

    @Test
    void isValid_WithNullValue_ShouldReturnTrue() {
        // Null is handled by @NotNull annotation, so we return true
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void isValid_WithEmptyString_ShouldReturnFalse() {
        assertFalse(validator.isValid("", null));
    }

    @Test
    void isValid_WithSpecialCharacters_ShouldReturnFalse() {
        assertFalse(validator.isValid("US$", null));
        assertFalse(validator.isValid("U.S", null));
        assertFalse(validator.isValid("US-D", null));
    }

    @Test
    void isValid_WithTooShortCode_ShouldReturnFalse() {
        assertFalse(validator.isValid("US", null));
        assertFalse(validator.isValid("U", null));
    }

    @Test
    void isValid_WithTooLongCode_ShouldReturnFalse() {
        assertFalse(validator.isValid("USDD", null));
        assertFalse(validator.isValid("DOLLAR", null));
    }
}