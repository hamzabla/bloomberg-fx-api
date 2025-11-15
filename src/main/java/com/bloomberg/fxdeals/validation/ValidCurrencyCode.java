package com.bloomberg.fxdeals.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for ISO 4217 currency codes.
 * Validates that the currency code is a valid 3-letter ISO code.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrencyCodeValidator.class)
@Documented
public @interface ValidCurrencyCode {

    String message() default "Invalid currency code. Must be a valid ISO 4217 code (e.g., USD, EUR, GBP)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}