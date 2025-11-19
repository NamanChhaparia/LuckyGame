package com.gaming.luckengine.repository;

import com.gaming.luckengine.domain.entity.Game;
import com.gaming.luckengine.domain.enums.GameStatus;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Game entity operations.
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Find game by game code.
     */
    Optional<Game> findByGameCode(String gameCode);

    /**
     * Find games by status.
     */
    List<Game> findByStatus(GameStatus status);

    /**
     * Find active games.
     */
    @Query("SELECT g FROM Game g WHERE g.status = 'ACTIVE' AND g.endTime > :currentTime")
    List<Game> findActiveGames(LocalDateTime currentTime);

    /**
     * Find latest active game.
     * Uses Spring Data JPA's Top keyword (automatically limits to 1 result).
     */
    Optional<Game> findTop1ByStatusOrderByStartTimeDesc(GameStatus status);
    
    /**
     * Convenience method to find latest active game.
     */
    default Optional<Game> findLatestActiveGame() {
        return findTop1ByStatusOrderByStartTimeDesc(GameStatus.ACTIVE);
    }

    /**
     * Find game with pessimistic lock for budget updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.id = :gameId")
    Optional<Game> findByIdWithLock(Long gameId);

    /**
     * Find games that should be started.
     */
    @Query("SELECT g FROM Game g WHERE g.status = 'SCHEDULED' AND g.startTime <= :currentTime")
    List<Game> findGamesToStart(LocalDateTime currentTime);

    /**
     * Find games that should be completed.
     */
    @Query("SELECT g FROM Game g WHERE g.status = 'ACTIVE' AND g.endTime <= :currentTime")
    List<Game> findGamesToComplete(LocalDateTime currentTime);
}

