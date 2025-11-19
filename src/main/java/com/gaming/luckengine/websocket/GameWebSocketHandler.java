package com.gaming.luckengine.websocket;

import com.gaming.luckengine.dto.RewardRequest;
import com.gaming.luckengine.dto.RewardResponse;
import com.gaming.luckengine.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time game interactions.
 * Implements the 1-second batch aggregation logic.
 * 
 * Flow:
 * 1. Clients connect via WebSocket
 * 2. Clients send play requests
 * 3. Requests are buffered for 1 second
 * 4. Every second, batch is sent to RewardService
 * 5. Results are broadcast back to clients
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketHandler {

    private final RewardService rewardService;
    private final SimpMessagingTemplate messagingTemplate;

    // Thread-safe buffer for accumulating requests
    private final ConcurrentHashMap<Long, List<String>> gameRequestBuffers = new ConcurrentHashMap<>();

    /**
     * Handle incoming play requests from clients.
     * Requests are buffered and not immediately processed.
     */
    @MessageMapping("/game/play")
    @SendTo("/topic/game/acknowledgment")
    public String handlePlayRequest(PlayRequest playRequest) {
        log.debug("Received play request from user: {} for game: {}", 
                playRequest.getUsername(), playRequest.getGameId());

        // Add to buffer
        gameRequestBuffers.computeIfAbsent(playRequest.getGameId(), 
                k -> Collections.synchronizedList(new ArrayList<>()))
                .add(playRequest.getUsername());

        return "Request received";
    }

    /**
     * Scheduled task that runs every 1 second to process batches.
     * This implements the time-windowed batch processing requirement.
     */
    @Scheduled(fixedRate = 1000) // Every 1 second
    public void processBatches() {
        if (gameRequestBuffers.isEmpty()) {
            return;
        }

        // Process each game's buffer
        gameRequestBuffers.forEach((gameId, usernames) -> {
            if (usernames.isEmpty()) {
                return;
            }

            // Create a snapshot and clear the buffer
            List<String> batchUsernames;
            synchronized (usernames) {
                batchUsernames = new ArrayList<>(usernames);
                usernames.clear();
            }

            if (batchUsernames.isEmpty()) {
                return;
            }

            log.info("Processing batch for game {} with {} users", gameId, batchUsernames.size());

            try {
                // Create batch request
                RewardRequest request = RewardRequest.builder()
                        .batchId("batch_" + UUID.randomUUID().toString())
                        .gameId(gameId)
                        .usernames(batchUsernames)
                        .timestamp(System.currentTimeMillis())
                        .build();

                // Process batch
                RewardResponse response = rewardService.processBatch(request);

                // Broadcast results to all connected clients
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/results", response);

                log.info("Batch processed and broadcast successfully");

            } catch (Exception e) {
                log.error("Error processing batch for game {}: {}", gameId, e.getMessage(), e);
            }
        });
    }

    /**
     * Inner class representing a play request from client.
     */
    public static class PlayRequest {
        private Long gameId;
        private String username;

        public PlayRequest() {}

        public PlayRequest(Long gameId, String username) {
            this.gameId = gameId;
            this.username = username;
        }

        public Long getGameId() {
            return gameId;
        }

        public void setGameId(Long gameId) {
            this.gameId = gameId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}

