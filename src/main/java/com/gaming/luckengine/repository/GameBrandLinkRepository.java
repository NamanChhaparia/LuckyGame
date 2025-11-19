package com.gaming.luckengine.repository;

import com.gaming.luckengine.domain.entity.GameBrandLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GameBrandLink entity operations.
 */
@Repository
public interface GameBrandLinkRepository extends JpaRepository<GameBrandLink, Long> {

    /**
     * Find all links for a specific game.
     */
    List<GameBrandLink> findByGameId(Long gameId);

    /**
     * Find all links for a specific brand.
     */
    List<GameBrandLink> findByBrandId(Long brandId);

    /**
     * Find specific link between game and brand.
     */
    Optional<GameBrandLink> findByGameIdAndBrandId(Long gameId, Long brandId);

    /**
     * Calculate total contribution for a game.
     */
    @Query("SELECT SUM(gbl.contributionAmount) FROM GameBrandLink gbl WHERE gbl.game.id = :gameId")
    java.math.BigDecimal calculateTotalContribution(Long gameId);
}

