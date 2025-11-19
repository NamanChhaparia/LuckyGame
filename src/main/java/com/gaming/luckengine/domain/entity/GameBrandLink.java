package com.gaming.luckengine.domain.entity;

import javax.persistence.*;
import javax.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents the budget allocation link between a Game and a Brand.
 * Tracks how much each brand contributes to a specific game.
 */
@Entity
@Table(name = "game_brand_links", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "brand_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameBrandLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Positive(message = "Contribution amount must be positive")
    @Column(name = "contribution_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal contributionAmount;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Lock the contribution to prevent modifications during active game.
     */
    public void lockContribution() {
        this.isLocked = true;
    }

    /**
     * Check if this link is modifiable.
     */
    public boolean isModifiable() {
        return !isLocked;
    }
}

