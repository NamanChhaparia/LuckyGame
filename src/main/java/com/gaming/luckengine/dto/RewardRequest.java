package com.gaming.luckengine.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch reward processing.
 * Represents a 1-second batch of user reward requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardRequest {

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Game ID is required")
    private Long gameId;

    @NotEmpty(message = "Usernames list cannot be empty")
    private List<String> usernames;

    private Long timestamp;
}

