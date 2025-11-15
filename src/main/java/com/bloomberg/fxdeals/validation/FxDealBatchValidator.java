package com.bloomberg.fxdeals.validation;

import com.bloomberg.fxdeals.dto.FxDealRequest;
import com.bloomberg.fxdeals.dto.FxDealBatchRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator service for FX Deal batch operations.
 * Provides validation logic separated from business logic.
 */
@Component
public class FxDealBatchValidator {

    private final Validator validator;

    public FxDealBatchValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates a single FX deal request.
     *
     * @param dealRequest the deal to validate
     * @return ValidationResult containing validation status and error messages
     */
    public ValidationResult validate(FxDealRequest dealRequest) {
        Set<ConstraintViolation<FxDealRequest>> violations = validator.validate(dealRequest);

        if (violations.isEmpty()) {
            return ValidationResult.valid();
        }

        String errorMessages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        return ValidationResult.invalid(errorMessages);
    }

    /**
     * Result of validation containing status and error messages.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isInvalid() {
            return !valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Validates the entire batch request structure.
     * Checks if the batch request is valid (not null, not empty).
     *
     * @param batchRequest the batch request to validate
     * @return ValidationResult for the batch structure
     */
    public ValidationResult validateBatchStructure(FxDealBatchRequest batchRequest) {
        if (batchRequest == null) {
            return ValidationResult.invalid("Batch request cannot be null");
        }

        if (batchRequest.getDeals() == null) {
            return ValidationResult.invalid("Deals list cannot be null");
        }

        if (batchRequest.getDeals().isEmpty()) {
            return ValidationResult.invalid("Deals list cannot be empty");
        }

        return ValidationResult.valid();
    }
}