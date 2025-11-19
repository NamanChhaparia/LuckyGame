package com.gaming.luckengine.service;

import com.gaming.luckengine.domain.entity.Brand;
import com.gaming.luckengine.domain.entity.Game;
import com.gaming.luckengine.domain.entity.GameBrandLink;
import com.gaming.luckengine.domain.enums.GameStatus;
import com.gaming.luckengine.exception.GameStateException;
import com.gaming.luckengine.exception.ResourceNotFoundException;
import com.gaming.luckengine.repository.GameBrandLinkRepository;
import com.gaming.luckengine.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for managing Game lifecycle.
 * Handles game creation, budget allocation, and state transitions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final GameBrandLinkRepository gameBrandLinkRepository;
    private final BrandService brandService;

    /**
     * Create a new game with brand contributions.
     * @param startTime When game should start
     * @param durationMinutes Duration of game in minutes
     * @param brandContributions Map of brand ID to contribution amount
     * @param winProbability Win probability (0.0 to 1.0)
     * @return Created game
     */
    @Transactional
    public Game createGame(LocalDateTime startTime, int durationMinutes,
                          Map<Long, BigDecimal> brandContributions, Double winProbability) {
        log.info("Creating game starting at: {} with {} brands", startTime, brandContributions.size());

        // Validate and calculate total budget
        BigDecimal totalBudget = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : brandContributions.entrySet()) {
            Brand brand = brandService.getBrandById(entry.getKey());
            BigDecimal contribution = entry.getValue();
            
            if (!brand.canAfford(contribution)) {
                throw new IllegalStateException(
                    String.format("Brand %s cannot afford contribution of %s", 
                        brand.getName(), contribution));
            }
            
            totalBudget = totalBudget.add(contribution);
        }

        // Create game
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        Game game = Game.builder()
                .startTime(startTime)
                .endTime(endTime)
                .totalBudget(totalBudget)
                .remainingBudget(totalBudget)
                .status(GameStatus.SCHEDULED)
                .winProbability(winProbability != null ? winProbability : 0.15)
                .volatilityFactor(1.2)
                .build();

        game = gameRepository.save(game);

        // Lock brand funds and create links
        for (Map.Entry<Long, BigDecimal> entry : brandContributions.entrySet()) {
            Brand brand = brandService.getBrandById(entry.getKey());
            BigDecimal contribution = entry.getValue();
            
            // Deduct from brand wallet (locked for game)
            brandService.deductFunds(brand.getId(), contribution);
            
            // Create link
            GameBrandLink link = GameBrandLink.builder()
                    .game(game)
                    .brand(brand)
                    .contributionAmount(contribution)
                    .isLocked(true)
                    .build();
            
            gameBrandLinkRepository.save(link);
        }

        log.info("Game created successfully with ID: {} and total budget: {}", game.getId(), totalBudget);
        return game;
    }

    /**
     * Get game by ID.
     */
    @Transactional(readOnly = true)
    public Game getGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + id));
    }

    /**
     * Get latest active game.
     */
    @Transactional(readOnly = true)
    public Game getLatestActiveGame() {
        return gameRepository.findLatestActiveGame()
                .orElseThrow(() -> new GameStateException("No active game found"));
    }

    /**
     * Get active games.
     */
    @Transactional(readOnly = true)
    public List<Game> getActiveGames() {
        return gameRepository.findActiveGames(LocalDateTime.now());
    }

    /**
     * Start a game manually.
     */
    @Transactional
    public Game startGame(Long gameId) {
        log.info("Starting game: {}", gameId);
        
        Game game = gameRepository.findByIdWithLock(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + gameId));

        if (game.getStatus() != GameStatus.SCHEDULED) {
            throw new GameStateException("Game must be in SCHEDULED status to start");
        }

        game.setStatus(GameStatus.ACTIVE);
        return gameRepository.save(game);
    }

    /**
     * Complete a game.
     */
    @Transactional
    public Game completeGame(Long gameId) {
        log.info("Completing game: {}", gameId);
        
        Game game = getGameById(gameId);
        
        if (game.getStatus() != GameStatus.ACTIVE) {
            throw new GameStateException("Only active games can be completed");
        }

        game.setStatus(GameStatus.COMPLETED);
        return gameRepository.save(game);
    }

    /**
     * Deduct budget from game atomically.
     */
    @Transactional
    public void deductGameBudget(Long gameId, BigDecimal amount) {
        Game game = gameRepository.findByIdWithLock(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + gameId));
        
        game.deductBudget(amount);
        gameRepository.save(game);
    }

    /**
     * Scheduled task to auto-start games.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void autoStartGames() {
        List<Game> gamesToStart = gameRepository.findGamesToStart(LocalDateTime.now());
        
        for (Game game : gamesToStart) {
            try {
                log.info("Auto-starting game: {}", game.getId());
                game.setStatus(GameStatus.ACTIVE);
                gameRepository.save(game);
            } catch (Exception e) {
                log.error("Error auto-starting game: {}", game.getId(), e);
            }
        }
    }

    /**
     * Scheduled task to auto-complete games.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void autoCompleteGames() {
        List<Game> gamesToComplete = gameRepository.findGamesToComplete(LocalDateTime.now());
        
        for (Game game : gamesToComplete) {
            try {
                log.info("Auto-completing game: {}", game.getId());
                game.setStatus(GameStatus.COMPLETED);
                gameRepository.save(game);
            } catch (Exception e) {
                log.error("Error auto-completing game: {}", game.getId(), e);
            }
        }
    }

    /**
     * Get all games.
     */
    @Transactional(readOnly = true)
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    /**
     * Get games by status.
     */
    @Transactional(readOnly = true)
    public List<Game> getGamesByStatus(GameStatus status) {
        return gameRepository.findByStatus(status);
    }
}

