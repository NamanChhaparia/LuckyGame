package com.gaming.luckengine.dto;

import com.gaming.luckengine.domain.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Individual user reward result within a batch response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRewardResult {

    private String username;
    private TransactionStatus status;
    private Long voucherId;
    private String voucherCode;
    private BigDecimal amount;
    private String message;
}

