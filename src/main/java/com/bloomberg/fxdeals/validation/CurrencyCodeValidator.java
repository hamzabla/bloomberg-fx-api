package com.bloomberg.fxdeals.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator implementation for currency codes.
 * Validates against ISO 4217 standard currency codes.
 */
public class CurrencyCodeValidator implements ConstraintValidator<ValidCurrencyCode, String> {

    private static final Set<String> VALID_CURRENCY_CODES = Currency.getAvailableCurrencies()
            .stream()
            .map(Currency::getCurrencyCode)
            .collect(Collectors.toSet());

    @Override
    public void initialize(ValidCurrencyCode constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull, so we consider null as valid here
        if (value == null) {
            return true;
        }

        // Check if the value is a valid ISO 4217 currency code
        return VALID_CURRENCY_CODES.contains(value.toUpperCase());
    }
}