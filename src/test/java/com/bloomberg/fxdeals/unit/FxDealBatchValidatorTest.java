package com.bloomberg.fxdeals.unit;

import com.bloomberg.fxdeals.dto.FxDealRequest;
import com.bloomberg.fxdeals.dto.FxDealBatchRequest;
import com.bloomberg.fxdeals.validation.FxDealBatchValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FxDealBatchValidator.
 */
class FxDealBatchValidatorTest {

    private FxDealBatchValidator batchValidator;
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        batchValidator = new FxDealBatchValidator(validator);
    }

    @Test
    void validate_WithValidDeal_ShouldReturnValid() {
        // Arrange
        FxDealRequest validRequest = FxDealRequest.builder()
                .dealUniqueId("TEST-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        // Act
        FxDealBatchValidator.ValidationResult result = batchValidator.validate(validRequest);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.isInvalid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void validate_WithMissingDealUniqueId_ShouldReturnInvalid() {
        // Arrange
        FxDealRequest invalidRequest = FxDealRequest.builder()
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        // Act
        FxDealBatchValidator.ValidationResult result = batchValidator.validate(invalidRequest);

        // Assert
        assertTrue(result.isInvalid());
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("required"));
    }

    @Test
    void validate_WithInvalidCurrencyCode_ShouldReturnInvalid() {
        // Arrange
        FxDealRequest invalidRequest = FxDealRequest.builder()
                .dealUniqueId("TEST-002")
                .fromCurrencyCode("INVALID")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        // Act
        FxDealBatchValidator.ValidationResult result = batchValidator.validate(invalidRequest);

        // Assert
        assertTrue(result.isInvalid());
        assertTrue(result.getErrorMessage().contains("Invalid"));
    }

    @Test
    void validate_WithNegativeAmount_ShouldReturnInvalid() {
        // Arrange
        FxDealRequest invalidRequest = FxDealRequest.builder()
                .dealUniqueId("TEST-003")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("-100.00"))
                .build();

        // Act
        FxDealBatchValidator.ValidationResult result = batchValidator.validate(invalidRequest);

        // Assert
        assertTrue(result.isInvalid());
        assertTrue(result.getErrorMessage().contains("greater than zero"));
    }

    @Test
    void validate_WithMultipleErrors_ShouldCombineMessages() {
        // Arrange
        FxDealRequest invalidRequest = FxDealRequest.builder()
                .fromCurrencyCode("INVALID")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("-100.00"))
                .build();

        // Act
        FxDealBatchValidator.ValidationResult result = batchValidator.validate(invalidRequest);

        // Assert
        assertTrue(result.isInvalid());
        assertNotNull(result.getErrorMessage());
        // Should contain multiple error messages
        assertTrue(result.getErrorMessage().length() > 20);
    }

    @Test
    void validationResult_Valid_ShouldHaveCorrectState() {
        // Act
        FxDealBatchValidator.ValidationResult result = FxDealBatchValidator.ValidationResult.valid();

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.isInvalid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void validationResult_Invalid_ShouldHaveCorrectState() {
        // Act
        String errorMessage = "Test error message";
        FxDealBatchValidator.ValidationResult result = FxDealBatchValidator.ValidationResult.invalid(errorMessage);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.isInvalid());
        assertEquals(errorMessage, result.getErrorMessage());
    }

    @Test
    void validateBatchStructure_WithValidBatchRequest_ShouldReturnValid() {
        // Arrange
        FxDealRequest validRequest = FxDealRequest.builder()
                .dealUniqueId("TEST-BATCH-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(java.util.Arrays.asList(validRequest))
                .build();

        // Act
        FxDealBatchValidator.ValidationResult result = batchValidator.validateBatchStructure(batchRequest);

        // Assert
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void validateBatchStructure_WithNullBatchRequest_ShouldReturnInvalid() {
        // Act
        FxDealBatchValidator.ValidationResult result = batchValidator.validateBatchStructure(null);

        // Assert
        assertTrue(result.isInvalid());
        assertEquals("Batch request cannot be null", result.getErrorMessage());
    }

    @Test
    void validateBatchStructure_WithNullDealsList_ShouldReturnInvalid() {
        // Arrange
        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(null)
                .build();

        // Act
        FxDealBatchValidator.ValidationResult result = batchValidator.validateBatchStructure(batchRequest);

        // Assert
        assertTrue(result.isInvalid());
        assertEquals("Deals list cannot be null", result.getErrorMessage());
    }

    @Test
    void validateBatchStructure_WithEmptyDealsList_ShouldReturnInvalid() {
        // Arrange
        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(java.util.Collections.emptyList())
                .build();

        // Act
        FxDealBatchValidator.ValidationResult result = batchValidator.validateBatchStructure(batchRequest);

        // Assert
        assertTrue(result.isInvalid());
        assertEquals("Deals list cannot be empty", result.getErrorMessage());
    }
}