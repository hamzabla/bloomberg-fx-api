package com.bloomberg.fxdeals.unit;

import com.bloomberg.fxdeals.dto.FxDealRequest;
import com.bloomberg.fxdeals.dto.FxDealResponse;
import com.bloomberg.fxdeals.dto.FxDealBatchRequest;
import com.bloomberg.fxdeals.dto.FxDealBatchResponse;
import com.bloomberg.fxdeals.validation.FxDealBatchValidator;
import com.bloomberg.fxdeals.exception.DealNotFoundException;
import com.bloomberg.fxdeals.exception.DuplicateDealException;
import com.bloomberg.fxdeals.model.FxDeal;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import com.bloomberg.fxdeals.service.FxDealService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FxDealService.
 * Tests business logic in isolation using mocks.
 */
@ExtendWith(MockitoExtension.class)
class FxDealServiceTest {

    @Mock
    private FxDealRepository fxDealRepository;

    @Mock
    private FxDealBatchValidator batchValidator;

    @InjectMocks
    private FxDealService fxDealService;

    private FxDealRequest validRequest;
    private FxDeal validDeal;

    @BeforeEach
    void setUp() {
        validRequest = FxDealRequest.builder()
                .dealUniqueId("DEAL-TEST-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.50"))
                .build();

        validDeal = FxDeal.builder()
                .id(1L)
                .dealUniqueId("DEAL-TEST-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.50"))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void createDeal_WithValidData_ShouldSucceed() {
        // Arrange
        when(fxDealRepository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(fxDealRepository.save(any(FxDeal.class))).thenReturn(validDeal);

        // Act
        FxDealResponse response = fxDealService.createDeal(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals("DEAL-TEST-001", response.getDealUniqueId());
        assertEquals("USD", response.getFromCurrencyCode());
        assertEquals("EUR", response.getToCurrencyCode());

        verify(fxDealRepository).existsByDealUniqueId("DEAL-TEST-001");
        verify(fxDealRepository).save(any(FxDeal.class));
    }

    @Test
    void createDeal_WithDuplicateId_ShouldThrowException() {
        // Arrange
        when(fxDealRepository.existsByDealUniqueId(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateDealException.class, () -> {
            fxDealService.createDeal(validRequest);
        });

        verify(fxDealRepository).existsByDealUniqueId("DEAL-TEST-001");
        verify(fxDealRepository, never()).save(any(FxDeal.class));
    }

    @Test
    void createDeal_WithSameCurrencyCodes_ShouldThrowException() {
        // Arrange
        validRequest.setToCurrencyCode("USD"); // Same as fromCurrencyCode
        when(fxDealRepository.existsByDealUniqueId(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            fxDealService.createDeal(validRequest);
        });

        verify(fxDealRepository, never()).save(any(FxDeal.class));
    }

    @Test
    void createDeal_ShouldConvertCurrencyCodeToUpperCase() {
        // Arrange
        validRequest.setFromCurrencyCode("usd");
        validRequest.setToCurrencyCode("eur");
        when(fxDealRepository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(fxDealRepository.save(any(FxDeal.class))).thenReturn(validDeal);

        // Act
        FxDealResponse response = fxDealService.createDeal(validRequest);

        // Assert
        assertEquals("USD", response.getFromCurrencyCode());
        assertEquals("EUR", response.getToCurrencyCode());
    }

    @Test
    void getAllDeals_ShouldReturnAllDeals() {
        // Arrange
        FxDeal deal2 = FxDeal.builder()
                .id(2L)
                .dealUniqueId("DEAL-TEST-002")
                .fromCurrencyCode("GBP")
                .toCurrencyCode("JPY")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("2000.00"))
                .createdAt(Instant.now())
                .build();

        when(fxDealRepository.findAll()).thenReturn(Arrays.asList(validDeal, deal2));

        // Act
        List<FxDealResponse> deals = fxDealService.getAllDeals();

        // Assert
        assertEquals(2, deals.size());
        assertEquals("DEAL-TEST-001", deals.get(0).getDealUniqueId());
        assertEquals("DEAL-TEST-002", deals.get(1).getDealUniqueId());

        verify(fxDealRepository).findAll();
    }

    @Test
    void getAllDeals_WhenEmpty_ShouldReturnEmptyList() {
        // Arrange
        when(fxDealRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<FxDealResponse> deals = fxDealService.getAllDeals();

        // Assert
        assertTrue(deals.isEmpty());
        verify(fxDealRepository).findAll();
    }

    @Test
    void getDealById_WithValidId_ShouldReturnDeal() {
        // Arrange
        when(fxDealRepository.findById(1L)).thenReturn(Optional.of(validDeal));

        // Act
        FxDealResponse response = fxDealService.getDealById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("DEAL-TEST-001", response.getDealUniqueId());

        verify(fxDealRepository).findById(1L);
    }

    @Test
    void getDealById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(fxDealRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DealNotFoundException.class, () -> {
            fxDealService.getDealById(999L);
        });

        verify(fxDealRepository).findById(999L);
    }

    @Test
    void getDealByUniqueId_WithValidId_ShouldReturnDeal() {
        // Arrange
        when(fxDealRepository.findByDealUniqueId("DEAL-TEST-001"))
                .thenReturn(Optional.of(validDeal));

        // Act
        FxDealResponse response = fxDealService.getDealByUniqueId("DEAL-TEST-001");

        // Assert
        assertNotNull(response);
        assertEquals("DEAL-TEST-001", response.getDealUniqueId());

        verify(fxDealRepository).findByDealUniqueId("DEAL-TEST-001");
    }

    @Test
    void getDealByUniqueId_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(fxDealRepository.findByDealUniqueId("INVALID-ID"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DealNotFoundException.class, () -> {
            fxDealService.getDealByUniqueId("INVALID-ID");
        });

        verify(fxDealRepository).findByDealUniqueId("INVALID-ID");
    }

    @Test
    void importBatch_WithAllValidDeals_ShouldSucceed() {
        // Arrange
        when(batchValidator.validateBatchStructure(any(FxDealBatchRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        when(batchValidator.validate(any(FxDealRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        FxDealRequest request1 = FxDealRequest.builder()
                .dealUniqueId("BATCH-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        FxDealRequest request2 = FxDealRequest.builder()
                .dealUniqueId("BATCH-002")
                .fromCurrencyCode("GBP")
                .toCurrencyCode("JPY")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("2000.00"))
                .build();

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(Arrays.asList(request1, request2))
                .build();

        when(fxDealRepository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(fxDealRepository.save(any(FxDeal.class))).thenReturn(validDeal);

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert
        assertEquals(2, response.getTotalRequests());
        assertEquals(2, response.getSuccessfulImports());
        assertEquals(0, response.getFailedImports());
        assertEquals(2, response.getSuccessfulDeals().size());
        assertEquals(0, response.getFailedDeals().size());

        verify(fxDealRepository, times(2)).save(any(FxDeal.class));
    }

    @Test
    void importBatch_WithMixedValidAndInvalid_ShouldProcessAll() {
        // Arrange - This tests the "NO ROLLBACK" requirement!
        when(batchValidator.validateBatchStructure(any(FxDealBatchRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        when(batchValidator.validate(any(FxDealRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        FxDealRequest validRequest = FxDealRequest.builder()
                .dealUniqueId("BATCH-VALID-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        FxDealRequest duplicateRequest = FxDealRequest.builder()
                .dealUniqueId("BATCH-DUPLICATE-001")
                .fromCurrencyCode("GBP")
                .toCurrencyCode("JPY")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("2000.00"))
                .build();

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(Arrays.asList(validRequest, duplicateRequest))
                .build();

        // First deal succeeds, second is duplicate
        when(fxDealRepository.existsByDealUniqueId("BATCH-VALID-001")).thenReturn(false);
        when(fxDealRepository.existsByDealUniqueId("BATCH-DUPLICATE-001")).thenReturn(true);
        when(fxDealRepository.save(any(FxDeal.class))).thenReturn(validDeal);

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert - Valid deal saved, duplicate rejected (NO ROLLBACK!)
        assertEquals(2, response.getTotalRequests());
        assertEquals(1, response.getSuccessfulImports());
        assertEquals(1, response.getFailedImports());
        assertEquals(1, response.getSuccessfulDeals().size());
        assertEquals(1, response.getFailedDeals().size());
        assertTrue(response.getFailedDeals().get(0).getReason().contains("Duplicate"));

        // Verify the valid deal was still saved even though another failed
        verify(fxDealRepository, times(1)).save(any(FxDeal.class));
    }

    @Test
    void importBatch_WithSameCurrencyError_ShouldContinueProcessing() {
        // Arrange
        when(batchValidator.validateBatchStructure(any(FxDealBatchRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        when(batchValidator.validate(any(FxDealRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        FxDealRequest validRequest = FxDealRequest.builder()
                .dealUniqueId("BATCH-VALID-002")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        FxDealRequest sameCurrencyRequest = FxDealRequest.builder()
                .dealUniqueId("BATCH-SAME-CURR-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("USD") // Same currency!
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("2000.00"))
                .build();

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(Arrays.asList(validRequest, sameCurrencyRequest))
                .build();

        when(fxDealRepository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(fxDealRepository.save(any(FxDeal.class))).thenReturn(validDeal);

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert
        assertEquals(2, response.getTotalRequests());
        assertEquals(1, response.getSuccessfulImports());
        assertEquals(1, response.getFailedImports());
        assertTrue(response.getFailedDeals().get(0).getReason().contains("different"));

        // Valid deal still saved despite other failing
        verify(fxDealRepository, times(1)).save(any(FxDeal.class));
    }

    @Test
    void importBatch_WithAllFailures_ShouldReturnAllFailed() {
        // Arrange
        when(batchValidator.validateBatchStructure(any(FxDealBatchRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        when(batchValidator.validate(any(FxDealRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        FxDealRequest duplicate1 = FxDealRequest.builder()
                .dealUniqueId("BATCH-DUP-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        FxDealRequest duplicate2 = FxDealRequest.builder()
                .dealUniqueId("BATCH-DUP-002")
                .fromCurrencyCode("GBP")
                .toCurrencyCode("JPY")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("2000.00"))
                .build();

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(Arrays.asList(duplicate1, duplicate2))
                .build();

        // All are duplicates
        when(fxDealRepository.existsByDealUniqueId(anyString())).thenReturn(true);

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert
        assertEquals(2, response.getTotalRequests());
        assertEquals(0, response.getSuccessfulImports());
        assertEquals(2, response.getFailedImports());
        assertEquals(0, response.getSuccessfulDeals().size());
        assertEquals(2, response.getFailedDeals().size());

        verify(fxDealRepository, never()).save(any(FxDeal.class));
    }

    @Test
    void importBatch_WithInvalidBatchStructure_ShouldReturnEmptyResponse() {
        // Arrange
        FxDealBatchRequest emptyBatchRequest = FxDealBatchRequest.builder()
                .deals(java.util.Collections.emptyList())
                .build();

        when(batchValidator.validateBatchStructure(any(FxDealBatchRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.invalid("Deals list cannot be empty"));

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(emptyBatchRequest);

        // Assert
        assertEquals(0, response.getTotalRequests());
        assertEquals(0, response.getSuccessfulImports());
        assertEquals(0, response.getFailedImports());
        assertTrue(response.getSuccessfulDeals().isEmpty());
        assertTrue(response.getFailedDeals().isEmpty());

        verify(fxDealRepository, never()).save(any(FxDeal.class));
    }

    @Test
    void importBatch_WithValidationException_ShouldAddToFailedList() {
        // Arrange
        when(batchValidator.validateBatchStructure(any(FxDealBatchRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        FxDealRequest invalidRequest = FxDealRequest.builder()
                .dealUniqueId("BATCH-INVALID-001")
                .fromCurrencyCode("INVALID")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(Arrays.asList(invalidRequest))
                .build();

        // Validation fails for this request
        when(batchValidator.validate(any(FxDealRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.invalid("Invalid currency code"));

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert
        assertEquals(1, response.getTotalRequests());
        assertEquals(0, response.getSuccessfulImports());
        assertEquals(1, response.getFailedImports());
        assertTrue(response.getFailedDeals().get(0).getReason().contains("Validation failed"));

        verify(fxDealRepository, never()).save(any(FxDeal.class));
    }

    @Test
    void importBatch_WithUnexpectedException_ShouldHandleGracefully() {
        // Arrange
        when(batchValidator.validateBatchStructure(any(FxDealBatchRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        when(batchValidator.validate(any(FxDealRequest.class)))
                .thenReturn(FxDealBatchValidator.ValidationResult.valid());

        FxDealRequest request = FxDealRequest.builder()
                .dealUniqueId("BATCH-ERROR-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(Arrays.asList(request))
                .build();

        when(fxDealRepository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(fxDealRepository.save(any(FxDeal.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert
        assertEquals(1, response.getTotalRequests());
        assertEquals(0, response.getSuccessfulImports());
        assertEquals(1, response.getFailedImports());
        assertTrue(response.getFailedDeals().get(0).getReason().contains("Error"));
    }
}