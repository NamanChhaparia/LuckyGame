package com.gaming.luckengine.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request DTO for creating a new game.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameCreateRequest {

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;

    @NotEmpty(message = "Brand contributions cannot be empty")
    private Map<Long, BigDecimal> brandContributions;

    private Double winProbability;
}

