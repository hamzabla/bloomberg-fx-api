package com.bloomberg.fxdeals.api;

import com.bloomberg.fxdeals.dto.FxDealRequest;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API Integration tests using RestAssured.
 * Tests the complete REST API against a real database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FxDealApiTest {

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
    void createDeal_WithValidData_ShouldReturn201() {
        FxDealRequest request = FxDealRequest.builder()
                .dealUniqueId("DEAL-API-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.parse("2024-11-13T10:00:00Z"))
                .dealAmount(new BigDecimal("1000.50"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("dealUniqueId", equalTo("DEAL-API-001"))
                .body("fromCurrencyCode", equalTo("USD"))
                .body("toCurrencyCode", equalTo("EUR"))
                .body("dealAmount", equalTo(1000.5f))
                .body("createdAt", notNullValue());
    }

    @Test
    void createDeal_WithMissingDealUniqueId_ShouldReturn400() {
        FxDealRequest request = FxDealRequest.builder()
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.50"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Validation Failed"))
                .body("fieldErrors.dealUniqueId", containsString("required"));
    }

    @Test
    void createDeal_WithMissingFromCurrency_ShouldReturn400() {
        FxDealRequest request = FxDealRequest.builder()
                .dealUniqueId("DEAL-API-002")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.50"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .body("fieldErrors.fromCurrencyCode", containsString("required"));
    }

    @Test
    void createDeal_WithInvalidCurrencyCode_ShouldReturn400() {
        FxDealRequest request = FxDealRequest.builder()
                .dealUniqueId("DEAL-API-003")
                .fromCurrencyCode("INVALID")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.50"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .body("fieldErrors.fromCurrencyCode", equalTo("Invalid from currency code"));
    }

    @Test
    void createDeal_WithNegativeAmount_ShouldReturn400() {
        FxDealRequest request = FxDealRequest.builder()
                .dealUniqueId("DEAL-API-004")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("-100.00"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .body("fieldErrors.dealAmount", containsString("greater than zero"));
    }

    @Test
    void createDeal_WithZeroAmount_ShouldReturn400() {
        FxDealRequest request = FxDealRequest.builder()
                .dealUniqueId("DEAL-API-005")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("0.00"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .body("fieldErrors.dealAmount", containsString("greater than zero"));
    }

    @Test
    void createDeal_WithFutureTimestamp_ShouldReturn400() {
        FxDealRequest request = FxDealRequest.builder()
                .dealUniqueId("DEAL-API-006")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.parse("2099-12-31T23:59:59Z"))
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .body("fieldErrors.dealTimestamp", containsString("cannot be in the future"));
    }

    @Test
    void createDeal_WithDuplicateId_ShouldReturn409() {
        // Create first deal
        FxDealRequest request1 = FxDealRequest.builder()
                .dealUniqueId("DEAL-API-DUPLICATE")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request1)
                .when()
                .post()
                .then()
                .statusCode(201);

        // Try to create duplicate
        FxDealRequest request2 = FxDealRequest.builder()
                .dealUniqueId("DEAL-API-DUPLICATE")
                .fromCurrencyCode("GBP")
                .toCurrencyCode("JPY")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("2000.00"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request2)
                .when()
                .post()
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("message", containsString("already exists"));
    }

    @Test
    void getAllDeals_WhenEmpty_ShouldReturnEmptyList() {
        given()
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void getAllDeals_WithMultipleDeals_ShouldReturnAll() {
        // Create multiple deals
        createDeal("DEAL-API-010", "USD", "EUR", "1000.00");
        createDeal("DEAL-API-011", "GBP", "JPY", "2000.00");
        createDeal("DEAL-API-012", "CHF", "CAD", "3000.00");

        given()
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("dealUniqueId", hasItems("DEAL-API-010", "DEAL-API-011", "DEAL-API-012"));
    }

    @Test
    void getDealById_WhenExists_ShouldReturn200() {
        // Create a deal
        String dealId = createDeal("DEAL-API-020", "USD", "EUR", "1000.00");

        given()
                .pathParam("id", dealId)
                .when()
                .get("/{id}")
                .then()
                .statusCode(200)
                .body("dealUniqueId", equalTo("DEAL-API-020"));
    }

    @Test
    void getDealById_WhenNotExists_ShouldReturn404() {
        given()
                .pathParam("id", 999)
                .when()
                .get("/{id}")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"));
    }

    @Test
    void getDealByUniqueId_WhenExists_ShouldReturn200() {
        // Create a deal
        createDeal("DEAL-API-030", "USD", "EUR", "1000.00");

        given()
                .pathParam("dealUniqueId", "DEAL-API-030")
                .when()
                .get("/unique/{dealUniqueId}")
                .then()
                .statusCode(200)
                .body("dealUniqueId", equalTo("DEAL-API-030"));
    }

    @Test
    void healthCheck_ShouldReturn200() {
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .body(equalTo("FX Deals API is running"));
    }

    // Helper method
    private String createDeal(String uniqueId, String from, String to, String amount) {
        FxDealRequest request = FxDealRequest.builder()
                .dealUniqueId(uniqueId)
                .fromCurrencyCode(from)
                .toCurrencyCode(to)
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal(amount))
                .build();

        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .path("id")
                .toString();
    }
}