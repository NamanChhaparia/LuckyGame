package com.gaming.luckengine.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new brand.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandCreateRequest {

    @NotBlank(message = "Brand name is required")
    private String name;

    @PositiveOrZero(message = "Initial balance must be non-negative")
    private BigDecimal initialBalance;

    @PositiveOrZero(message = "Daily spend limit must be non-negative")
    private BigDecimal dailySpendLimit;
}

