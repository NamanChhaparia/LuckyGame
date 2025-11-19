package com.gaming.luckengine.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a reward voucher that can be won by users.
 * Each voucher has a cost, inventory count, and belongs to a brand.
 */
@Entity
@Table(name = "vouchers", indexes = {
    @Index(name = "idx_brand_active", columnList = "brand_id, is_active"),
    @Index(name = "idx_current_quantity", columnList = "current_quantity")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Voucher code is required")
    @Column(name = "voucher_code", nullable = false, unique = true, length = 100)
    private String voucherCode;

    @NotBlank(message = "Description is required")
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Positive(message = "Cost must be positive")
    @Column(name = "cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal cost;

    @PositiveOrZero(message = "Initial quantity cannot be negative")
    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @PositiveOrZero(message = "Current quantity cannot be negative")
    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    @JsonIgnore  // Prevent lazy loading during JSON serialization
    private Brand brand;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version; // Optimistic locking for inventory management

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentQuantity == null) {
            currentQuantity = initialQuantity;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if voucher is available for distribution.
     */
    public boolean isAvailable() {
        return isActive && 
               currentQuantity > 0 && 
               (expiryDate == null || LocalDateTime.now().isBefore(expiryDate));
    }

    /**
     * Decrement inventory by one.
     * @throws IllegalStateException if no inventory available
     */
    public synchronized void decrementInventory() {
        if (currentQuantity <= 0) {
            throw new IllegalStateException(
                String.format("No inventory available for voucher: %s", voucherCode));
        }
        this.currentQuantity--;
    }

    /**
     * Decrement inventory by specified quantity.
     */
    public synchronized void decrementInventory(int quantity) {
        if (currentQuantity < quantity) {
            throw new IllegalStateException(
                String.format("Insufficient inventory. Available: %d, Required: %d", 
                    currentQuantity, quantity));
        }
        this.currentQuantity -= quantity;
    }

    /**
     * Check if voucher can fulfill the requested quantity.
     */
    public boolean hasInventory(int quantity) {
        return currentQuantity >= quantity;
    }

    /**
     * Get total value of voucher based on cost and quantity.
     */
    public BigDecimal getTotalValue() {
        return cost.multiply(BigDecimal.valueOf(currentQuantity));
    }
}

