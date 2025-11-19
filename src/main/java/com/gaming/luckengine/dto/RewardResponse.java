package com.gaming.luckengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for batch reward processing.
 * Contains individual results for each user in the batch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardResponse {

    private String batchId;
    private LocalDateTime processedAt;
    private List<UserRewardResult> rewards;
    private BigDecimal totalSpent;
    private Long processingTimeMs;
}

