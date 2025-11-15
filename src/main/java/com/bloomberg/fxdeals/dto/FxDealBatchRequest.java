package com.bloomberg.fxdeals.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch import of FX deals.
 * Contains a list of deals to be imported.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxDealBatchRequest {

    @NotEmpty(message = "Deals list cannot be empty")
    @Valid
    private List<FxDealRequest> deals;
}