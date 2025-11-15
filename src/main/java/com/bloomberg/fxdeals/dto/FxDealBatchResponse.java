package com.bloomberg.fxdeals.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch import of FX deals.
 * Contains successful and failed imports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxDealBatchResponse {

    private int totalRequests;
    private int successfulImports;
    private int failedImports;
    private List<FxDealResponse> successfulDeals;
    private List<FailedDealImport> failedDeals;

    /**
     * Represents a failed deal import with error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedDealImport {
        private String dealUniqueId;
        private String reason;
        private FxDealRequest originalRequest;
    }
}