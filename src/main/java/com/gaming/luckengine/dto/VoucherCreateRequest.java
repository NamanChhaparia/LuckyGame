package com.gaming.luckengine.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a new voucher.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherCreateRequest {

    @NotNull(message = "Brand ID is required")
    private Long brandId;

    @NotBlank(message = "Voucher code is required")
    private String voucherCode;

    @NotBlank(message = "Description is required")
    private String description;

    @Positive(message = "Cost must be positive")
    private BigDecimal cost;

    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private LocalDateTime expiryDate;
}

