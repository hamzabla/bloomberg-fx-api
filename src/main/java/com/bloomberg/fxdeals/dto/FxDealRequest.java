package com.bloomberg.fxdeals.dto;

import com.bloomberg.fxdeals.validation.ValidCurrencyCode;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request DTO for creating a new FX deal.
 * Contains validation annotations for all fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxDealRequest {

    @NotBlank(message = "Deal unique ID is required")
    @Size(max = 100, message = "Deal unique ID must not exceed 100 characters")
    private String dealUniqueId;

    @NotBlank(message = "From currency code is required")
    @ValidCurrencyCode(message = "Invalid from currency code")
    private String fromCurrencyCode;

    @NotBlank(message = "To currency code is required")
    @ValidCurrencyCode(message = "Invalid to currency code")
    private String toCurrencyCode;

    @NotNull(message = "Deal timestamp is required")
    @PastOrPresent(message = "Deal timestamp cannot be in the future")
    private Instant dealTimestamp;

    @NotNull(message = "Deal amount is required")
    @DecimalMin(value = "0.01", message = "Deal amount must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Deal amount format is invalid")
    private BigDecimal dealAmount;
}