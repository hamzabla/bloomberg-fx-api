package com.bloomberg.fxdeals.integration;

import com.bloomberg.fxdeals.model.FxDeal;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FxDealRepository.
 * Tests actual database operations using Testcontainers.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FxDealRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private FxDealRepository fxDealRepository;

    private FxDeal testDeal;

    @BeforeEach
    void setUp() {
        fxDealRepository.deleteAll();

        testDeal = FxDeal.builder()
                .dealUniqueId("DEAL-IT-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.50"))
                .build();
    }

    @Test
    void save_WithValidDeal_ShouldPersistToDatabase() {
        // Act
        FxDeal saved = fxDealRepository.save(testDeal);

        // Assert
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals("DEAL-IT-001", saved.getDealUniqueId());
        assertEquals("USD", saved.getFromCurrencyCode());
        assertEquals("EUR", saved.getToCurrencyCode());
    }

    @Test
    void existsByDealUniqueId_WhenExists_ShouldReturnTrue() {
        // Arrange
        fxDealRepository.save(testDeal);

        // Act
        boolean exists = fxDealRepository.existsByDealUniqueId("DEAL-IT-001");

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByDealUniqueId_WhenNotExists_ShouldReturnFalse() {
        // Act
        boolean exists = fxDealRepository.existsByDealUniqueId("NON-EXISTENT");

        // Assert
        assertFalse(exists);
    }

    @Test
    void findByDealUniqueId_WhenExists_ShouldReturnDeal() {
        // Arrange
        fxDealRepository.save(testDeal);

        // Act
        Optional<FxDeal> found = fxDealRepository.findByDealUniqueId("DEAL-IT-001");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("DEAL-IT-001", found.get().getDealUniqueId());
    }

    @Test
    void findByDealUniqueId_WhenNotExists_ShouldReturnEmpty() {
        // Act
        Optional<FxDeal> found = fxDealRepository.findByDealUniqueId("NON-EXISTENT");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void findAll_ShouldReturnAllDeals() {
        // Arrange
        FxDeal deal1 = FxDeal.builder()
                .dealUniqueId("DEAL-IT-001")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        FxDeal deal2 = FxDeal.builder()
                .dealUniqueId("DEAL-IT-002")
                .fromCurrencyCode("GBP")
                .toCurrencyCode("JPY")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("2000.00"))
                .build();

        fxDealRepository.save(deal1);
        fxDealRepository.save(deal2);

        // Act
        List<FxDeal> deals = fxDealRepository.findAll();

        // Assert
        assertEquals(2, deals.size());
    }

    @Test
    void findById_WhenExists_ShouldReturnDeal() {
        // Arrange
        FxDeal saved = fxDealRepository.save(testDeal);

        // Act
        Optional<FxDeal> found = fxDealRepository.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Act
        Optional<FxDeal> found = fxDealRepository.findById(999L);

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void save_WithDuplicateUniqueId_ShouldThrowException() {
        // Arrange
        FxDeal deal1 = FxDeal.builder()
                .dealUniqueId("DEAL-IT-DUPLICATE")
                .fromCurrencyCode("USD")
                .toCurrencyCode("EUR")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();

        FxDeal deal2 = FxDeal.builder()
                .dealUniqueId("DEAL-IT-DUPLICATE")
                .fromCurrencyCode("GBP")
                .toCurrencyCode("JPY")
                .dealTimestamp(Instant.now())
                .dealAmount(new BigDecimal("2000.00"))
                .build();

        fxDealRepository.save(deal1);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            fxDealRepository.save(deal2);
            fxDealRepository.flush(); // Force the constraint check
        });
    }

    @Test
    void save_ShouldSetCreatedAtAutomatically() {
        // Act
        FxDeal saved = fxDealRepository.save(testDeal);

        // Assert
        assertNotNull(saved.getCreatedAt());
        assertTrue(saved.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
    }
}