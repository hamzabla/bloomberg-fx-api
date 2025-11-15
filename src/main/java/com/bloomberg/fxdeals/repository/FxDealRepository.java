package com.bloomberg.fxdeals.repository;

import com.bloomberg.fxdeals.model.FxDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for FxDeal entity.
 */
@Repository
public interface FxDealRepository extends JpaRepository<FxDeal, Long> {

    /**
     * Check if a deal with the given unique ID already exists.
     * Used to prevent duplicate imports.
     *
     * @param dealUniqueId the unique identifier of the deal
     * @return true if exists, false otherwise
     */
    boolean existsByDealUniqueId(String dealUniqueId);

    /**
     * Find a deal by its unique business identifier.
     *
     * @param dealUniqueId the unique identifier of the deal
     * @return Optional containing the deal if found
     */
    Optional<FxDeal> findByDealUniqueId(String dealUniqueId);
}