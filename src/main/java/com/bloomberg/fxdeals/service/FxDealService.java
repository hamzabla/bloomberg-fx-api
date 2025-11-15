package com.bloomberg.fxdeals.service;

import com.bloomberg.fxdeals.dto.FxDealRequest;
import com.bloomberg.fxdeals.dto.FxDealResponse;
import com.bloomberg.fxdeals.dto.FxDealBatchRequest;
import com.bloomberg.fxdeals.dto.FxDealBatchResponse;
import com.bloomberg.fxdeals.validation.FxDealBatchValidator;
import com.bloomberg.fxdeals.exception.DealNotFoundException;
import com.bloomberg.fxdeals.exception.DuplicateDealException;
import com.bloomberg.fxdeals.model.FxDeal;
import com.bloomberg.fxdeals.repository.FxDealRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * Service layer for FX Deal operations.
 * Contains business logic for creating and retrieving deals.
 */
@Service
@Transactional
public class FxDealService {

    private static final Logger logger = LoggerFactory.getLogger(FxDealService.class);

    private final FxDealRepository fxDealRepository;
    private final FxDealBatchValidator batchValidator;

    public FxDealService(FxDealRepository fxDealRepository,FxDealBatchValidator batchValidator) {
        this.fxDealRepository = fxDealRepository;
        this.batchValidator = batchValidator;
    }

    /**
     * Create a new FX deal.
     * Validates that the deal doesn't already exist.
     *
     * @param request the deal creation request
     * @return the created deal response
     * @throws DuplicateDealException if a deal with the same unique ID already exists
     */
    public FxDealResponse createDeal(FxDealRequest request) {
        logger.info("Creating new deal with ID: {}", request.getDealUniqueId());

        // Check for duplicate
        if (fxDealRepository.existsByDealUniqueId(request.getDealUniqueId())) {
            logger.warn("Duplicate deal detected: {}", request.getDealUniqueId());
            throw new DuplicateDealException(
                    request.getDealUniqueId(),
                    "This deal has already been imported"
            );
        }

        // Validate currency codes are different
        if (request.getFromCurrencyCode().equalsIgnoreCase(request.getToCurrencyCode())) {
            logger.warn("Same currency exchange attempted: {}", request.getFromCurrencyCode());
            throw new IllegalArgumentException("From and To currency codes must be different");
        }

        // Convert request to entity
        FxDeal deal = FxDeal.builder()
                .dealUniqueId(request.getDealUniqueId())
                .fromCurrencyCode(request.getFromCurrencyCode().toUpperCase())
                .toCurrencyCode(request.getToCurrencyCode().toUpperCase())
                .dealTimestamp(request.getDealTimestamp())
                .dealAmount(request.getDealAmount())
                .build();

        // Save to database
        FxDeal savedDeal = fxDealRepository.save(deal);

        logger.info("Deal saved successfully with database ID: {}", savedDeal.getId());

        // Convert entity to response
        return mapToResponse(savedDeal);
    }

    /**
     * Get all FX deals.
     *
     * @return list of all deals
     */
    @Transactional(readOnly = true)
    public List<FxDealResponse> getAllDeals() {
        logger.info("Retrieving all deals");
        List<FxDeal> deals = fxDealRepository.findAll();
        logger.info("Found {} deals", deals.size());

        return deals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a deal by its database ID.
     *
     * @param id the database ID
     * @return the deal response
     * @throws DealNotFoundException if deal not found
     */
    @Transactional(readOnly = true)
    public FxDealResponse getDealById(Long id) {
        logger.info("Retrieving deal with ID: {}", id);

        FxDeal deal = fxDealRepository.findById(id)
                .orElseThrow(() -> new DealNotFoundException(id));

        return mapToResponse(deal);
    }

    /**
     * Get a deal by its unique business identifier.
     *
     * @param dealUniqueId the unique deal identifier
     * @return the deal response
     * @throws DealNotFoundException if deal not found
     */
    @Transactional(readOnly = true)
    public FxDealResponse getDealByUniqueId(String dealUniqueId) {
        logger.info("Retrieving deal with unique ID: {}", dealUniqueId);

        FxDeal deal = fxDealRepository.findByDealUniqueId(dealUniqueId)
                .orElseThrow(() -> new DealNotFoundException(dealUniqueId, true));

        return mapToResponse(deal);
    }

    /**
     * Map FxDeal entity to FxDealResponse DTO.
     */
    private FxDealResponse mapToResponse(FxDeal deal) {
        return FxDealResponse.builder()
                .id(deal.getId())
                .dealUniqueId(deal.getDealUniqueId())
                .fromCurrencyCode(deal.getFromCurrencyCode())
                .toCurrencyCode(deal.getToCurrencyCode())
                .dealTimestamp(deal.getDealTimestamp())
                .dealAmount(deal.getDealAmount())
                .createdAt(deal.getCreatedAt())
                .build();
    }

    /**
     * Import a batch of FX deals.
     * Each deal is processed independently - no rollback.
     * Valid deals are saved even if some deals fail validation.
     *
     * @param batchRequest the batch import request
     * @return batch response with successful and failed imports
     */
    public FxDealBatchResponse importBatch(FxDealBatchRequest batchRequest) {
        // Validate batch structure first
        FxDealBatchValidator.ValidationResult structureValidation = batchValidator.validateBatchStructure(batchRequest);

        if (structureValidation.isInvalid()) {
            logger.warn("Batch import rejected: {}", structureValidation.getErrorMessage());

            // Return empty response for invalid structure
            return FxDealBatchResponse.builder()
                    .totalRequests(0)
                    .successfulImports(0)
                    .failedImports(0)
                    .successfulDeals(new ArrayList<>())
                    .failedDeals(new ArrayList<>())
                    .build();
        }
        logger.info("Starting batch import of {} deals", batchRequest.getDeals().size());

        List<FxDealResponse> successfulDeals = new ArrayList<>();
        List<FxDealBatchResponse.FailedDealImport> failedDeals = new ArrayList<>();

        for (FxDealRequest dealRequest : batchRequest.getDeals()) {
            try {
                // Validate each deal using the validation service
                FxDealBatchValidator.ValidationResult validationResult = batchValidator.validate(dealRequest);

                if (validationResult.isInvalid()) {
                    // Validation failed
                    FxDealBatchResponse.FailedDealImport failed = FxDealBatchResponse.FailedDealImport.builder()
                            .dealUniqueId(dealRequest.getDealUniqueId())
                            .reason("Validation failed: " + validationResult.getErrorMessage())
                            .originalRequest(dealRequest)
                            .build();
                    failedDeals.add(failed);
                    logger.warn("Failed to import deal {}: {}", dealRequest.getDealUniqueId(), validationResult.getErrorMessage());
                    continue; // Skip to next deal
                }
                // Process each deal independently
                FxDealResponse response = createDeal(dealRequest);
                successfulDeals.add(response);
                logger.debug("Deal imported successfully: {}", dealRequest.getDealUniqueId());
            } catch (DuplicateDealException e) {
                // Duplicate deal - add to failed list
                FxDealBatchResponse.FailedDealImport failed = FxDealBatchResponse.FailedDealImport.builder()
                        .dealUniqueId(dealRequest.getDealUniqueId())
                        .reason("Duplicate: " + e.getMessage())
                        .originalRequest(dealRequest)
                        .build();
                failedDeals.add(failed);
                logger.warn("Failed to import deal {}: {}", dealRequest.getDealUniqueId(), e.getMessage());
            } catch (IllegalArgumentException e) {
                // Business rule violation (e.g., same currency)
                FxDealBatchResponse.FailedDealImport failed = FxDealBatchResponse.FailedDealImport.builder()
                        .dealUniqueId(dealRequest.getDealUniqueId())
                        .reason("Validation error: " + e.getMessage())
                        .originalRequest(dealRequest)
                        .build();
                failedDeals.add(failed);
                logger.warn("Failed to import deal {}: {}", dealRequest.getDealUniqueId(), e.getMessage());
            } catch (Exception e) {
                // Any other error
                FxDealBatchResponse.FailedDealImport failed = FxDealBatchResponse.FailedDealImport.builder()
                        .dealUniqueId(dealRequest.getDealUniqueId())
                        .reason("Error: " + e.getMessage())
                        .originalRequest(dealRequest)
                        .build();
                failedDeals.add(failed);
                logger.error("Unexpected error importing deal {}: ", dealRequest.getDealUniqueId(), e);
            }
        }

        int total = batchRequest.getDeals().size();
        int successful = successfulDeals.size();
        int failed = failedDeals.size();

        logger.info("Batch import completed: {} total, {} successful, {} failed",
                total, successful, failed);

        return FxDealBatchResponse.builder()
                .totalRequests(total)
                .successfulImports(successful)
                .failedImports(failed)
                .successfulDeals(successfulDeals)
                .failedDeals(failedDeals)
                .build();
    }
}