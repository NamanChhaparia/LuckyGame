package com.gaming.luckengine.repository;

import com.gaming.luckengine.domain.entity.RewardTransaction;
import com.gaming.luckengine.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for RewardTransaction entity operations.
 */
@Repository
public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {

    /**
     * Find transactions by batch ID.
     */
    List<RewardTransaction> findByBatchId(String batchId);

    /**
     * Find transactions by user.
     */
    List<RewardTransaction> findByUserId(Long userId);

    /**
     * Find transactions by game.
     */
    List<RewardTransaction> findByGameId(Long gameId);

    /**
     * Find transactions by user and game.
     */
    List<RewardTransaction> findByUserIdAndGameId(Long userId, Long gameId);

    /**
     * Find winning transactions for a user.
     */
    List<RewardTransaction> findByUserIdAndStatus(Long userId, TransactionStatus status);

    /**
     * Count wins for a specific game.
     */
    @Query("SELECT COUNT(rt) FROM RewardTransaction rt WHERE rt.game.id = :gameId AND rt.status = 'WIN'")
    Long countWinsByGame(Long gameId);

    /**
     * Calculate total rewards distributed for a game.
     */
    @Query("SELECT SUM(rt.amount) FROM RewardTransaction rt WHERE rt.game.id = :gameId AND rt.status = 'WIN'")
    java.math.BigDecimal calculateTotalRewardsForGame(Long gameId);

    /**
     * Find transactions within date range.
     */
    @Query("SELECT rt FROM RewardTransaction rt WHERE rt.createdAt BETWEEN :startDate AND :endDate")
    List<RewardTransaction> findTransactionsBetweenDates(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Check if batch was already processed (idempotency).
     */
    boolean existsByBatchId(String batchId);
}

