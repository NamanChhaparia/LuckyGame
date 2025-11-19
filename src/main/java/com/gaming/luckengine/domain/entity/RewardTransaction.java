package com.gaming.luckengine.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gaming.luckengine.domain.enums.TransactionStatus;
import javax.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an audit log of reward transactions.
 * Records every reward attempt and its outcome.
 */
@Entity
@Table(name = "reward_transactions", indexes = {
    @Index(name = "idx_user_game", columnList = "user_id, game_id"),
    @Index(name = "idx_batch_id", columnList = "batch_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore  // Prevent lazy loading during JSON serialization
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    @JsonIgnore  // Prevent lazy loading during JSON serialization
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    @JsonIgnore  // Prevent lazy loading during JSON serialization
    private Voucher voucher;

    @Column(name = "batch_id", nullable = false, length = 50)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "reward_message", length = 500)
    private String rewardMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Check if transaction was successful.
     */
    public boolean isWin() {
        return status == TransactionStatus.WIN;
    }

    /**
     * Check if transaction was a loss.
     */
    public boolean isLoss() {
        return status == TransactionStatus.LOSS;
    }
}

