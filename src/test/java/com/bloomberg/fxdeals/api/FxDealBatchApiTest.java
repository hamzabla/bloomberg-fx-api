package com.bloomberg.fxdeals.api;

import com.bloomberg.fxdeals.dto.FxDealBatchRequest;
import com.bloomberg.fxdeals.dto.FxDealRequest;
import com.bloomberg.fxdeals.integration.AbstractIntegrationTest;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API Integration tests for batch import functionality.
 * Tests the "no rollback" requirement - valid deals saved even if some fail.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FxDealBatchApiTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private FxDealRepository fxDealRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/deals";

        // Clean database before each test
        fxDealRepository.deleteAll();
    }

    @Test
    void importBatch_WithAllValidDeals_ShouldReturn201() {
        List<FxDealRequest> deals = Arrays.asList(
                createDealRequest("BATCH-001", "USD", "EUR", "1000.00"),
                createDealRequest("BATCH-002", "GBP", "JPY", "2000.00"),
                createDealRequest("BATCH-003", "CHF", "CAD", "3000.00")
        );

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(deals)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(batchRequest)
                .when()
                .post("/batch")
                .then()
                .statusCode(201)
                .body("totalRequests", equalTo(3))
                .body("successfulImports", equalTo(3))
                .body("failedImports", equalTo(0))
                .body("successfulDeals", hasSize(3))
                .body("failedDeals", hasSize(0));
    }

    @Test
    void importBatch_WithMixedValidAndInvalid_ShouldReturn207AndSaveValidOnes() {
        // This is the KEY test for "no rollback" requirement!
        List<FxDealRequest> deals = Arrays.asList(
                createDealRequest("BATCH-MIXED-001", "USD", "EUR", "1000.00"),    // Valid
                createDealRequest("BATCH-MIXED-002", "INVALID", "EUR", "2000.00"), // Invalid currency
                createDealRequest("BATCH-MIXED-003", "GBP", "JPY", "3000.00"),     // Valid
                createDealRequest("BATCH-MIXED-004", "USD", "EUR", "-100.00"),     // Negative amount (will be caught by validation)
                createDealRequest("BATCH-MIXED-005", "CHF", "CAD", "5000.00")      // Valid
        );

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(deals)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(batchRequest)
                .when()
                .post("/batch")
                .then()
                .statusCode(207) // Multi-Status
                .body("totalRequests", equalTo(5))
                .body("successfulImports", equalTo(3)) // 3 valid deals saved
                .body("failedImports", greaterThanOrEqualTo(1)) // At least 1 failed
                .body("successfulDeals", hasSize(3))
                .body("successfulDeals.dealUniqueId",
                        hasItems("BATCH-MIXED-001", "BATCH-MIXED-003", "BATCH-MIXED-005"));

        // Verify valid deals are in database (NO ROLLBACK!)
        long count = fxDealRepository.count();
        assert count == 3 : "Expected 3 deals in database, found " + count;
    }

    @Test
    void importBatch_WithDuplicates_ShouldSaveFirstAndRejectSecond() {
        List<FxDealRequest> deals = Arrays.asList(
                createDealRequest("BATCH-DUP-001", "USD", "EUR", "1000.00"),
                createDealRequest("BATCH-DUP-002", "GBP", "JPY", "2000.00"),
                createDealRequest("BATCH-DUP-001", "CHF", "CAD", "3000.00") // Duplicate ID
        );

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(deals)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(batchRequest)
                .when()
                .post("/batch")
                .then()
                .statusCode(207) // Multi-Status
                .body("totalRequests", equalTo(3))
                .body("successfulImports", equalTo(2))
                .body("failedImports", equalTo(1))
                .body("failedDeals[0].dealUniqueId", equalTo("BATCH-DUP-001"))
                .body("failedDeals[0].reason", containsString("Duplicate"));

        // Verify only 2 deals in database
        long count = fxDealRepository.count();
        assert count == 2 : "Expected 2 deals in database, found " + count;
    }

    @Test
    void importBatch_WithAllInvalidDeals_ShouldReturn400() {
        List<FxDealRequest> deals = Arrays.asList(
                createDealRequest("BATCH-INV-001", "INVALID", "EUR", "1000.00"),
                createDealRequest("BATCH-INV-002", "USD", "INVALID", "2000.00")
        );

        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(deals)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(batchRequest)
                .when()
                .post("/batch")
                .then()
                .statusCode(400) // All failed
                .body("totalRequests", equalTo(2))
                .body("successfulImports", equalTo(0))
                .body("failedImports", equalTo(2));
    }

    @Test
    void importBatch_WithEmptyList_ShouldReturn400() {
        FxDealBatchRequest batchRequest = FxDealBatchRequest.builder()
                .deals(Arrays.asList())
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(batchRequest)
                .when()
                .post("/batch")
                .then()
                .statusCode(400)
                .body("totalRequests", equalTo(0))
                .body("successfulImports", equalTo(0))
                .body("failedImports", equalTo(0));
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