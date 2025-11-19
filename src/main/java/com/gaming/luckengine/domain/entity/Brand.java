package com.gaming.luckengine.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a Brand entity that sponsors games and provides vouchers.
 * Each brand has a wallet balance and daily spending limits.
 */
@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Brand name is required")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @PositiveOrZero(message = "Wallet balance cannot be negative")
    @Column(name = "wallet_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal walletBalance;

    @PositiveOrZero(message = "Daily spend limit cannot be negative")
    @Column(name = "daily_spend_limit", precision = 19, scale = 2)
    private BigDecimal dailySpendLimit;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore  // Prevent lazy loading during JSON serialization
    private Set<Voucher> vouchers = new HashSet<>();

    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore  // Prevent lazy loading during JSON serialization
    private Set<GameBrandLink> gameBrandLinks = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (walletBalance == null) {
            walletBalance = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Deduct amount from wallet balance.
     * @param amount Amount to deduct
     * @throws IllegalStateException if insufficient balance
     */
    public void deductFromWallet(BigDecimal amount) {
        if (walletBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                String.format("Insufficient balance. Available: %s, Required: %s", 
                    walletBalance, amount));
        }
        this.walletBalance = this.walletBalance.subtract(amount);
    }

    /**
     * Add amount to wallet balance.
     * @param amount Amount to add
     */
    public void addToWallet(BigDecimal amount) {
        this.walletBalance = this.walletBalance.add(amount);
    }

    /**
     * Check if brand can afford the given amount.
     * @param amount Amount to check
     * @return true if brand has sufficient balance
     */
    public boolean canAfford(BigDecimal amount) {
        return walletBalance.compareTo(amount) >= 0;
    }
}

