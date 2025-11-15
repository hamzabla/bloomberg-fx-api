package com.bloomberg.fxdeals.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for FX deal.
 * Returned to clients after successful operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxDealResponse {

    private Long id;
    private String dealUniqueId;
    private String fromCurrencyCode;
    private String toCurrencyCode;
    private Instant dealTimestamp;
    private BigDecimal dealAmount;
    private Instant createdAt;
}