package com.gaming.luckengine.controller;

import com.gaming.luckengine.domain.entity.RewardTransaction;
import com.gaming.luckengine.dto.RewardRequest;
import com.gaming.luckengine.dto.RewardResponse;
import com.gaming.luckengine.service.RewardService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Reward processing operations.
 * This is the core endpoint for batch reward processing.
 */
@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    /**
     * Process a batch of reward requests.
     * This endpoint receives a 1-second batch of users and processes their rewards.
     */
    @PostMapping("/process-batch")
    public ResponseEntity<RewardResponse> processBatch(@Valid @RequestBody RewardRequest request) {
        RewardResponse response = rewardService.processBatch(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction history for a specific user.
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<RewardTransaction>> getUserTransactions(@PathVariable Long userId) {
        List<RewardTransaction> transactions = rewardService.getUserTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get transaction history for a specific game.
     */
    @GetMapping("/game/{gameId}/history")
    public ResponseEntity<List<RewardTransaction>> getGameTransactions(@PathVariable Long gameId) {
        List<RewardTransaction> transactions = rewardService.getGameTransactionHistory(gameId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get statistics for a specific game.
     */
    @GetMapping("/game/{gameId}/statistics")
    public ResponseEntity<Map<String, Object>> getGameStatistics(@PathVariable Long gameId) {
        Map<String, Object> statistics = rewardService.getGameStatistics(gameId);
        return ResponseEntity.ok(statistics);
    }
}

