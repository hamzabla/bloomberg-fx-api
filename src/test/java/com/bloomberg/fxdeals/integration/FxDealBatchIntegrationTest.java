package com.bloomberg.fxdeals.integration;

import com.bloomberg.fxdeals.dto.FxDealBatchRequest;
import com.bloomberg.fxdeals.dto.FxDealBatchResponse;
import com.bloomberg.fxdeals.dto.FxDealRequest;
import com.bloomberg.fxdeals.model.FxDeal;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import com.bloomberg.fxdeals.service.FxDealService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for batch import functionality.
 * Tests with real database to verify NO ROLLBACK behavior.
 */
@SpringBootTest
@Transactional
class FxDealBatchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FxDealService fxDealService;

    @Autowired
    private FxDealRepository fxDealRepository;

    @BeforeEach
    void setUp() {
        fxDealRepository.deleteAll();
    }

    @Test
    void importBatch_WithAllValid_ShouldSaveAllToDatabase() {
        // Arrange
        List<FxDealRequest> deals = Arrays.asList(
                createDealRequest("INT-BATCH-001", "USD", "EUR", "1000.00"),
                createDealRequest("INT-BATCH-002", "GBP", "JPY", "2000.00"),
                createDealRequest("INT-BATCH-003", "CHF", "CAD", "3000.00")
        );

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(deals)
                .build();

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert
        assertEquals(3, response.getSuccessfulImports());
        assertEquals(0, response.getFailedImports());

        // Verify all are in database
        List<FxDeal> savedDeals = fxDealRepository.findAll();
        assertEquals(3, savedDeals.size());
    }

    @Test
    void importBatch_WithMixedValidAndInvalid_ShouldSaveOnlyValid() {
        // Arrange - KEY TEST for NO ROLLBACK requirement!
        List<FxDealRequest> deals = Arrays.asList(
                createDealRequest("INT-BATCH-VALID-001", "USD", "EUR", "1000.00"),    // Valid
                createDealRequest("INT-BATCH-SAME-001", "USD", "USD", "2000.00"),     // Same currency - invalid
                createDealRequest("INT-BATCH-VALID-002", "GBP", "JPY", "3000.00")     // Valid
        );

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(deals)
                .build();

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert
        assertEquals(3, response.getTotalRequests());
        assertEquals(2, response.getSuccessfulImports());
        assertEquals(1, response.getFailedImports());

        // CRITICAL: Verify valid deals ARE in database despite one failing (NO ROLLBACK!)
        List<FxDeal> savedDeals = fxDealRepository.findAll();
        assertEquals(2, savedDeals.size());

        assertTrue(fxDealRepository.existsByDealUniqueId("INT-BATCH-VALID-001"));
        assertTrue(fxDealRepository.existsByDealUniqueId("INT-BATCH-VALID-002"));
        assertFalse(fxDealRepository.existsByDealUniqueId("INT-BATCH-SAME-001"));
    }

    @Test
    void importBatch_WithDuplicates_ShouldSaveFirstRejectSecond() {
        // Arrange
        List<FxDealRequest> deals = Arrays.asList(
                createDealRequest("INT-BATCH-DUP-001", "USD", "EUR", "1000.00"),
                createDealRequest("INT-BATCH-DUP-002", "GBP", "JPY", "2000.00"),
                createDealRequest("INT-BATCH-DUP-001", "CHF", "CAD", "3000.00") // Duplicate!
        );

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(deals)
                .build();

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert
        assertEquals(3, response.getTotalRequests());
        assertEquals(2, response.getSuccessfulImports());
        assertEquals(1, response.getFailedImports());
        assertTrue(response.getFailedDeals().get(0).getReason().contains("Duplicate"));

        // Verify only 2 in database
        List<FxDeal> savedDeals = fxDealRepository.findAll();
        assertEquals(2, savedDeals.size());
    }

    @Test
    void importBatch_WithDatabaseConstraintViolation_ShouldContinueWithOthers() {
        // Arrange - First create a deal to cause duplicate constraint
        FxDeal existingDeal = FxDeal.builder()
                .dealUniqueId("INT-BATCH-EXISTING")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("5000.00"))
                .build();
        fxDealRepository.save(existingDeal);

        List<FxDealRequest> deals = Arrays.asList(
                createDealRequest("INT-BATCH-NEW-001", "USD", "EUR", "1000.00"),      // Valid
                createDealRequest("INT-BATCH-EXISTING", "GBP", "JPY", "2000.00"),     // Duplicate!
                createDealRequest("INT-BATCH-NEW-002", "CHF", "CAD", "3000.00")       // Valid
        );

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(deals)
                .build();

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert - Valid deals saved despite duplicate
        assertEquals(3, response.getTotalRequests());
        assertEquals(2, response.getSuccessfulImports());
        assertEquals(1, response.getFailedImports());

        // Verify: 1 existing + 2 new = 3 total
        List<FxDeal> savedDeals = fxDealRepository.findAll();
        assertEquals(3, savedDeals.size());
    }

    @Test
    void importBatch_PartialSuccess_VerifiesNoRollback() {
        // Arrange - This is the DEFINITIVE test for NO ROLLBACK!
        // Mix of: valid, duplicate, invalid, valid
        List<FxDealRequest> deals = Arrays.asList(
                createDealRequest("NO-ROLLBACK-001", "USD", "EUR", "1000.00"),   // Valid - should be saved
                createDealRequest("NO-ROLLBACK-002", "GBP", "GBP", "2000.00"),   // Invalid (same currency)
                createDealRequest("NO-ROLLBACK-003", "CHF", "CAD", "3000.00"),   // Valid - should be saved
                createDealRequest("NO-ROLLBACK-004", "JPY", "AUD", "4000.00")    // Valid - should be saved
        );

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(deals)
                .build();

        // Act
        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        // Assert
        assertEquals(4, response.getTotalRequests());
        assertEquals(3, response.getSuccessfulImports());
        assertEquals(1, response.getFailedImports());

        // CRITICAL: All 3 valid deals MUST be in database
        // Even though one deal in the middle failed!
        List<FxDeal> savedDeals = fxDealRepository.findAll();
        assertEquals(3, savedDeals.size());

        assertTrue(fxDealRepository.existsByDealUniqueId("NO-ROLLBACK-001"));
        assertFalse(fxDealRepository.existsByDealUniqueId("NO-ROLLBACK-002"));
        assertTrue(fxDealRepository.existsByDealUniqueId("NO-ROLLBACK-003"));
        assertTrue(fxDealRepository.existsByDealUniqueId("NO-ROLLBACK-004"));
    }

    // Helper method
    private FxDealRequest createDealRequest(String uniqueId, String from, String to, String amount) {
        return FxDealRequest.builder()
                .dealUniqueId(uniqueId)
                .fromCurrencyCode(from)
                .toCurrencyCode(to)
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal(amount))
                .build();
    }
}