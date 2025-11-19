package com.gaming.luckengine.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gaming.luckengine.domain.enums.GameStatus;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a Game event session where users can play and win rewards.
 * Each game has a fixed budget and time window.
 */
@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_code", unique = true, nullable = false, length = 50)
    private String gameCode;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Positive(message = "Total budget must be positive")
    @Column(name = "total_budget", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "remaining_budget", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBudget;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GameStatus status = GameStatus.SCHEDULED;

    @Column(name = "win_probability", nullable = false)
    @Builder.Default
    private Double winProbability = 0.15; // Default 15% win rate

    @Column(name = "volatility_factor", nullable = false)
    @Builder.Default
    private Double volatilityFactor = 1.2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore  // Prevent lazy loading during JSON serialization
    private Set<GameBrandLink> gameBrandLinks = new HashSet<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore  // Prevent lazy loading during JSON serialization
    private Set<RewardTransaction> transactions = new HashSet<>();

    @Version
    private Long version; // Optimistic locking

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (remainingBudget == null) {
            remainingBudget = totalBudget;
        }
        if (gameCode == null) {
            gameCode = generateGameCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate the budget available for the current second (tick).
     * Formula: (RemainingBudget / RemainingSeconds) * VolatilityFactor
     */
    public BigDecimal calculateTickBudget() {
        if (status != GameStatus.ACTIVE) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime) || remainingBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        long remainingSeconds = java.time.Duration.between(now, endTime).getSeconds();
        if (remainingSeconds <= 0) {
            return remainingBudget;
        }

        BigDecimal budgetPerSecond = remainingBudget.divide(
            BigDecimal.valueOf(remainingSeconds), 
            2, 
            java.math.RoundingMode.HALF_UP
        );

        return budgetPerSecond.multiply(BigDecimal.valueOf(volatilityFactor));
    }

    /**
     * Deduct amount from remaining budget.
     */
    public synchronized void deductBudget(BigDecimal amount) {
        if (remainingBudget.compareTo(amount) < 0) {
            throw new IllegalStateException(
                String.format("Insufficient game budget. Available: %s, Required: %s", 
                    remainingBudget, amount));
        }
        this.remainingBudget = this.remainingBudget.subtract(amount);
    }

    /**
     * Check if game is currently active and has budget.
     */
    public boolean isActiveAndFunded() {
        return status == GameStatus.ACTIVE && 
               remainingBudget.compareTo(BigDecimal.ZERO) > 0 &&
               LocalDateTime.now().isBefore(endTime);
    }

    /**
     * Get remaining duration in seconds.
     */
    public long getRemainingSeconds() {
        if (LocalDateTime.now().isAfter(endTime)) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
    }

    private String generateGameCode() {
        return "GAME_" + System.currentTimeMillis();
    }
}

