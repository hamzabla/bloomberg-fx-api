package com.bloomberg.fxdeals.controller;

import com.bloomberg.fxdeals.dto.FxDealRequest;
import com.bloomberg.fxdeals.dto.FxDealResponse;
import com.bloomberg.fxdeals.dto.FxDealBatchRequest;
import com.bloomberg.fxdeals.dto.FxDealBatchResponse;
import com.bloomberg.fxdeals.service.FxDealService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for FX Deal operations.
 * Provides endpoints for creating and retrieving deals.
 */
@RestController
@RequestMapping("/api/deals")
public class FxDealController {

    private static final Logger logger = LoggerFactory.getLogger(FxDealController.class);

    private final FxDealService fxDealService;

    public FxDealController(FxDealService fxDealService) {
        this.fxDealService = fxDealService;
    }

    /**
     * Create a new FX deal.
     *
     * POST /api/deals
     *
     * @param request the deal creation request
     * @return the created deal with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<FxDealResponse> createDeal(@Valid @RequestBody FxDealRequest request) {
        logger.info("Received request to create deal: {}", request.getDealUniqueId());

        FxDealResponse response = fxDealService.createDeal(request);

        logger.info("Deal created successfully: {}", response.getId());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all FX deals.
     *
     * GET /api/deals
     *
     * @return list of all deals
     */
    @GetMapping
    public ResponseEntity<List<FxDealResponse>> getAllDeals() {
        logger.info("Received request to get all deals");

        List<FxDealResponse> deals = fxDealService.getAllDeals();

        logger.info("Returning {} deals", deals.size());

        return ResponseEntity.ok(deals);
    }

    /**
     * Get a deal by its database ID.
     *
     * GET /api/deals/{id}
     *
     * @param id the database ID
     * @return the deal
     */
    @GetMapping("/{id}")
    public ResponseEntity<FxDealResponse> getDealById(@PathVariable Long id) {
        logger.info("Received request to get deal with ID: {}", id);

        FxDealResponse deal = fxDealService.getDealById(id);

        return ResponseEntity.ok(deal);
    }

    /**
     * Get a deal by its unique business identifier.
     *
     * GET /api/deals/unique/{dealUniqueId}
     *
     * @param dealUniqueId the unique deal identifier
     * @return the deal
     */
    @GetMapping("/unique/{dealUniqueId}")
    public ResponseEntity<FxDealResponse> getDealByUniqueId(@PathVariable String dealUniqueId) {
        logger.info("Received request to get deal with unique ID: {}", dealUniqueId);

        FxDealResponse deal = fxDealService.getDealByUniqueId(dealUniqueId);

        return ResponseEntity.ok(deal);
    }

    /**
     * Import a batch of FX deals.
     * Each deal is processed independently (no rollback).
     * Valid deals are saved even if some fail.
     *
     * POST /api/deals/batch
     *
     * @param batchRequest the batch import request
     * @return batch response with successful and failed imports
     */
    @PostMapping("/batch")
    public ResponseEntity<FxDealBatchResponse> importBatch(@RequestBody FxDealBatchRequest batchRequest) {
        logger.info("Received batch import request");

        FxDealBatchResponse response = fxDealService.importBatch(batchRequest);

        logger.info("Batch import completed: {} successful, {} failed",
                response.getSuccessfulImports(), response.getFailedImports());

        // Return 207 Multi-Status if there are both successes and failures
        // Return 201 Created if all succeeded
        // Return 400 Bad Request if all failed
        HttpStatus status;
        if (response.getSuccessfulImports() > 0 && response.getFailedImports() > 0) {
            status = HttpStatus.MULTI_STATUS; // 207
        } else if (response.getSuccessfulImports() > 0) {
            status = HttpStatus.CREATED; // 201
        } else {
            status = HttpStatus.BAD_REQUEST; // 400
        }

        return new ResponseEntity<>(response, status);
    }

    /**
     * Health check endpoint.
     *
     * GET /api/deals/health
     *
     * @return simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("FX Deals API is running");
    }
}