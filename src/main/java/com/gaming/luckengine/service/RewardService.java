package com.gaming.luckengine.service;

import com.gaming.luckengine.domain.entity.Game;
import com.gaming.luckengine.domain.entity.RewardTransaction;
import com.gaming.luckengine.domain.entity.User;
import com.gaming.luckengine.domain.entity.Voucher;
import com.gaming.luckengine.domain.enums.GameStatus;
import com.gaming.luckengine.domain.enums.TransactionStatus;
import com.gaming.luckengine.dto.RewardRequest;
import com.gaming.luckengine.dto.RewardResponse;
import com.gaming.luckengine.dto.UserRewardResult;
import com.gaming.luckengine.exception.GameStateException;
import com.gaming.luckengine.repository.GameRepository;
import com.gaming.luckengine.repository.RewardTransactionRepository;
import com.gaming.luckengine.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core Reward Service implementing the Time-Windowed Batch Processing System.
 * This is the heart of the luck-based gaming engine that ensures strict budget compliance.
 * 
 * Algorithm Flow:
 * 1. Receive batch of user requests
 * 2. Validate game is active and funded
 * 3. Calculate tick budget (budget for current second)
 * 4. Fetch candidate vouchers within budget
 * 5. Randomize user order (Fisher-Yates shuffle)
 * 6. For each user: determine win/loss and assign voucher if won
 * 7. Atomically update inventory and budget
 * 8. Return results
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private final GameRepository gameRepository;
    private final VoucherRepository voucherRepository;
    private final RewardTransactionRepository transactionRepository;
    private final UserService userService;
    private final Random random = new Random();

    /**
     * Process a batch of reward requests.
     * This is the core method implementing the SRS specification.
     * 
     * @param request Batch request containing game ID, batch ID, and list of usernames
     * @return Batch response with individual results for each user
     */
    @Transactional
    public RewardResponse processBatch(RewardRequest request) {
        return processBatchWithRetry(request, 3);
    }

    /**
     * Process batch with retry logic for optimistic locking conflicts.
     */
    private RewardResponse processBatchWithRetry(RewardRequest request, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return processBatchInternal(request);
            } catch (OptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("Failed to process batch {} after {} retries due to optimistic locking conflict", 
                            request.getBatchId(), maxRetries);
                    throw new GameStateException("Unable to process batch due to concurrent modifications. Please retry.");
                }
                log.warn("Optimistic locking conflict on batch {}, retrying (attempt {}/{})", 
                        request.getBatchId(), attempt, maxRetries);
                // Brief delay before retry
                try {
                    Thread.sleep(10 + (attempt * 5)); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new GameStateException("Batch processing interrupted");
                }
            }
        }
        throw new GameStateException("Failed to process batch after retries");
    }

    /**
     * Internal batch processing method.
     */
    private RewardResponse processBatchInternal(RewardRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing batch: {} for game: {} with {} users", 
                request.getBatchId(), request.getGameId(), request.getUsernames().size());

        // Step 1: Idempotency check
        if (transactionRepository.existsByBatchId(request.getBatchId())) {
            log.warn("Batch {} already processed. Returning cached results.", request.getBatchId());
            return getCachedBatchResults(request.getBatchId());
        }

        // Step 2: Global checks - Game status and budget (with pessimistic lock)
        Game game = gameRepository.findByIdWithLock(request.getGameId())
                .orElseThrow(() -> new GameStateException("Game not found: " + request.getGameId()));

        if (!game.isActiveAndFunded()) {
            log.warn("Game {} is not active or has no budget. Returning all losses.", game.getId());
            return createAllLossResponse(request, game);
        }

        // Step 3: Calculate Tick Budget using Dynamic Decay Model
        // Re-read game state to ensure we have latest remainingBudget
        BigDecimal remainingBudget = game.getRemainingBudget();
        BigDecimal tickBudget = game.calculateTickBudget();
        log.info("Tick budget calculated: {} (Remaining: {}, Seconds left: {})", 
                tickBudget, remainingBudget, game.getRemainingSeconds());

        // Step 4: Fetch candidate vouchers within budget constraint
        // Note: Vouchers will be locked individually when decrementing inventory
        List<Voucher> candidateVouchers = voucherRepository
                .findAvailableVouchersWithinBudget(tickBudget);

        if (candidateVouchers.isEmpty()) {
            log.warn("No available vouchers within tick budget. Returning all losses.");
            return createAllLossResponse(request, game);
        }

        // Step 5: Randomization - Shuffle users for fairness (Fisher-Yates)
        List<String> shuffledUsernames = new ArrayList<>(request.getUsernames());
        Collections.shuffle(shuffledUsernames, random);

        // Step 6: Process each user in randomized order
        List<UserRewardResult> results = new ArrayList<>();
        BigDecimal currentBatchSpend = BigDecimal.ZERO;

        // Use index-based loop to avoid inefficient indexOf() calls
        for (int userIndex = 0; userIndex < shuffledUsernames.size(); userIndex++) {
            String username = shuffledUsernames.get(userIndex);
            User user = userService.getOrCreateUser(username, null, null);
            
            // Re-read game state before each user to ensure budget accuracy
            // This prevents overspending when multiple batches run concurrently
            game = gameRepository.findByIdWithLock(request.getGameId())
                    .orElseThrow(() -> new GameStateException("Game not found: " + request.getGameId()));
            
            if (!game.isActiveAndFunded()) {
                log.info("Game {} became inactive during batch processing. Processing remaining users as losses.", game.getId());
                // Process remaining users as losses
                for (int i = userIndex; i < shuffledUsernames.size(); i++) {
                    User remainingUser = userService.getOrCreateUser(shuffledUsernames.get(i), null, null);
                    results.add(createLossResult(remainingUser, game, request.getBatchId()));
                }
                break;
            }

            // Recalculate tickBudget with latest game state
            BigDecimal currentRemainingBudget = game.getRemainingBudget();
            BigDecimal currentTickBudget = game.calculateTickBudget();
            
            // Pass budget values instead of game object to avoid stale data
            UserRewardResult result = processUserReward(
                    user, game, request.getGameId(), request.getBatchId(), 
                    candidateVouchers, currentTickBudget, 
                    currentBatchSpend, currentRemainingBudget, game.getWinProbability());

            results.add(result);

            // Update current batch spend if user won
            if (result.getStatus() == TransactionStatus.WIN) {
                currentBatchSpend = currentBatchSpend.add(result.getAmount());
            }

            // Early termination if budget exhausted
            // Use the latest remainingBudget from database
            if (currentBatchSpend.compareTo(currentTickBudget) >= 0 || 
                currentBatchSpend.compareTo(currentRemainingBudget) >= 0) {
                log.info("Batch budget exhausted. Processing remaining users as losses.");
                // Process remaining users as losses
                for (int i = userIndex + 1; i < shuffledUsernames.size(); i++) {
                    User remainingUser = userService.getOrCreateUser(shuffledUsernames.get(i), null, null);
                    results.add(createLossResult(remainingUser, game, request.getBatchId()));
                }
                break;
            }
        }

        // Step 7: Atomically update game budget
        // Re-read game with lock to ensure we have latest state before deducting
        if (currentBatchSpend.compareTo(BigDecimal.ZERO) > 0) {
            game = gameRepository.findByIdWithLock(request.getGameId())
                    .orElseThrow(() -> new GameStateException("Game not found: " + request.getGameId()));
            
            // Calculate actual spend from transactions to ensure consistency
            // This prevents any discrepancy between currentBatchSpend and actual transaction amounts
            BigDecimal actualSpend = results.stream()
                    .filter(r -> r.getStatus() == TransactionStatus.WIN)
                    .map(UserRewardResult::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Use actual spend from transactions instead of tracked spend
            BigDecimal spendToDeduct = actualSpend;
            
            // Double-check budget is still sufficient
            if (game.getRemainingBudget().compareTo(spendToDeduct) < 0) {
                log.error("CRITICAL: Insufficient budget for batch {}: required {}, available {}. " +
                        "This should not happen with proper locking. Using available budget.", 
                        request.getBatchId(), spendToDeduct, game.getRemainingBudget());
                // This should never happen with proper pessimistic locking, but if it does,
                // we use the available budget to prevent negative budget
                spendToDeduct = game.getRemainingBudget();
                
                // Log warning if there's a discrepancy
                if (spendToDeduct.compareTo(actualSpend) < 0) {
                    log.error("Budget adjustment required: transactions total {}, but only {} available. " +
                            "This indicates a race condition that should have been prevented by locking.",
                            actualSpend, spendToDeduct);
                }
            }
            
            if (spendToDeduct.compareTo(BigDecimal.ZERO) > 0) {
                game.deductBudget(spendToDeduct);
                gameRepository.save(game);
                
                // Update currentBatchSpend to match actual deduction
                currentBatchSpend = spendToDeduct;
                
                // Check if game budget is exhausted
                if (game.getRemainingBudget().compareTo(BigDecimal.ZERO) <= 0) {
                    game.setStatus(GameStatus.BUDGET_EXHAUSTED);
                    gameRepository.save(game);
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Batch {} processed in {}ms. Total spend: {}, Winners: {}", 
                request.getBatchId(), duration, currentBatchSpend, 
                results.stream().filter(r -> r.getStatus() == TransactionStatus.WIN).count());

        return RewardResponse.builder()
                .batchId(request.getBatchId())
                .processedAt(LocalDateTime.now())
                .rewards(results)
                .totalSpent(currentBatchSpend)
                .processingTimeMs(duration)
                .build();
    }

    /**
     * Process individual user reward decision.
     * Implements the randomization and constraint checking logic.
     * Uses pessimistic locking on vouchers to prevent inventory race conditions.
     * 
     * @param user The user to process
     * @param game The game entity (for transaction creation)
     * @param gameId The game ID (for re-reading game state if needed)
     * @param batchId The batch ID
     * @param candidateVouchers List of candidate vouchers
     * @param tickBudget Current tick budget
     * @param currentBatchSpend Current batch spend so far
     * @param currentRemainingBudget Current remaining game budget (to avoid stale game object)
     * @param winProbability Win probability for the game
     * @return UserRewardResult with the outcome
     */
    private UserRewardResult processUserReward(User user, Game game, Long gameId, String batchId,
                                               List<Voucher> candidateVouchers,
                                               BigDecimal tickBudget,
                                               BigDecimal currentBatchSpend,
                                               BigDecimal currentRemainingBudget,
                                               Double winProbability) {
        // Generate random seed for win/loss decision
        double randomValue = random.nextDouble();

        // Check if user wins based on probability
        if (randomValue > winProbability) {
            // User loses
            return createLossResult(user, game, batchId);
        }

        // User is a potential winner - try to assign voucher
        // Shuffle vouchers for random selection
        List<Voucher> shuffledVouchers = new ArrayList<>(candidateVouchers);
        Collections.shuffle(shuffledVouchers, random);

        for (Voucher candidateVoucher : shuffledVouchers) {
            // Constraint checks on candidate voucher (preliminary check)
            BigDecimal potentialSpend = currentBatchSpend.add(candidateVoucher.getCost());
            
            boolean withinTickBudget = potentialSpend.compareTo(tickBudget) <= 0;
            // Use currentRemainingBudget parameter instead of stale game object
            boolean withinGameBudget = potentialSpend.compareTo(currentRemainingBudget) <= 0;

            if (!withinTickBudget || !withinGameBudget) {
                continue; // Skip this voucher - doesn't fit budget
            }

            // CRITICAL: Lock voucher before checking availability and decrementing
            // This prevents race conditions where multiple batches try to use the same voucher
            try {
                Voucher lockedVoucher = voucherRepository.findByIdWithLock(candidateVoucher.getId())
                        .orElse(null);
                
                if (lockedVoucher == null) {
                    continue; // Voucher no longer exists
                }

                // Re-check availability with locked voucher (most up-to-date state)
                boolean hasInventory = lockedVoucher.isAvailable();
                BigDecimal lockedVoucherCost = lockedVoucher.getCost();
                
                // Re-check budget constraints with locked voucher cost (in case it changed)
                // Re-read game state to get latest budget (critical for accuracy)
                Game currentGame = gameRepository.findByIdWithLock(gameId)
                        .orElseThrow(() -> new GameStateException("Game not found: " + gameId));
                BigDecimal latestRemainingBudget = currentGame.getRemainingBudget();
                
                BigDecimal lockedPotentialSpend = currentBatchSpend.add(lockedVoucherCost);
                boolean lockedWithinTickBudget = lockedPotentialSpend.compareTo(tickBudget) <= 0;
                boolean lockedWithinGameBudget = lockedPotentialSpend.compareTo(latestRemainingBudget) <= 0;

                if (hasInventory && lockedWithinTickBudget && lockedWithinGameBudget) {
                    // All constraints pass - atomically decrement inventory
                    lockedVoucher.decrementInventory();
                    voucherRepository.save(lockedVoucher);

                    // Create win transaction
                    RewardTransaction transaction = RewardTransaction.builder()
                            .user(user)
                            .game(currentGame)
                            .voucher(lockedVoucher)
                            .batchId(batchId)
                            .status(TransactionStatus.WIN)
                            .amount(lockedVoucherCost)
                            .rewardMessage("Congratulations! You won: " + lockedVoucher.getDescription())
                            .build();

                    transactionRepository.save(transaction);

                    return UserRewardResult.builder()
                            .username(user.getUsername())
                            .status(TransactionStatus.WIN)
                            .voucherId(lockedVoucher.getId())
                            .voucherCode(lockedVoucher.getVoucherCode())
                            .amount(lockedVoucherCost)
                            .message("Congratulations! You won: " + lockedVoucher.getDescription())
                            .build();

                } else {
                    // Voucher no longer available or doesn't fit budget - try next one
                    log.debug("Voucher {} not available or doesn't fit budget. Available: {}, Within tick: {}, Within game: {}", 
                            lockedVoucher.getId(), hasInventory, lockedWithinTickBudget, lockedWithinGameBudget);
                }
            } catch (Exception e) {
                log.error("Error assigning voucher {} to user {}: {}", 
                        candidateVoucher.getId(), user.getUsername(), e.getMessage(), e);
                // Continue to next voucher
            }
        }

        // No suitable voucher found - user loses
        return createLossResult(user, game, batchId);
    }

    /**
     * Create a loss result for a user.
     */
    private UserRewardResult createLossResult(User user, Game game, String batchId) {
        RewardTransaction transaction = RewardTransaction.builder()
                .user(user)
                .game(game)
                .batchId(batchId)
                .status(TransactionStatus.LOSS)
                .rewardMessage("Better luck next time!")
                .build();

        transactionRepository.save(transaction);

        return UserRewardResult.builder()
                .username(user.getUsername())
                .status(TransactionStatus.LOSS)
                .message("Better luck next time!")
                .build();
    }

    /**
     * Create response with all losses (when game is not active/funded).
     */
    private RewardResponse createAllLossResponse(RewardRequest request, Game game) {
        List<UserRewardResult> results = new ArrayList<>();
        
        for (String username : request.getUsernames()) {
            User user = userService.getOrCreateUser(username, null, null);
            results.add(createLossResult(user, game, request.getBatchId()));
        }

        return RewardResponse.builder()
                .batchId(request.getBatchId())
                .processedAt(LocalDateTime.now())
                .rewards(results)
                .totalSpent(BigDecimal.ZERO)
                .processingTimeMs(0L)
                .build();
    }

    /**
     * Get cached batch results for idempotency.
     */
    private RewardResponse getCachedBatchResults(String batchId) {
        List<RewardTransaction> transactions = transactionRepository.findByBatchId(batchId);
        
        List<UserRewardResult> results = transactions.stream()
                .map(t -> UserRewardResult.builder()
                        .username(t.getUser().getUsername())
                        .status(t.getStatus())
                        .voucherId(t.getVoucher() != null ? t.getVoucher().getId() : null)
                        .voucherCode(t.getVoucher() != null ? t.getVoucher().getVoucherCode() : null)
                        .amount(t.getAmount())
                        .message(t.getRewardMessage())
                        .build()).collect(Collectors.toList());

        BigDecimal totalSpent = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.WIN)
                .map(RewardTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RewardResponse.builder()
                .batchId(batchId)
                .processedAt(LocalDateTime.now())
                .rewards(results)
                .totalSpent(totalSpent)
                .processingTimeMs(0L)
                .build();
    }

    /**
     * Get transaction history for a user.
     */
    @Transactional(readOnly = true)
    public List<RewardTransaction> getUserTransactionHistory(Long userId) {
        return transactionRepository.findByUserId(userId);
    }

    /**
     * Get transaction history for a game.
     */
    @Transactional(readOnly = true)
    public List<RewardTransaction> getGameTransactionHistory(Long gameId) {
        return transactionRepository.findByGameId(gameId);
    }

    /**
     * Get statistics for a game.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGameStatistics(Long gameId) {
        Long totalWins = transactionRepository.countWinsByGame(gameId);
        BigDecimal totalRewards = transactionRepository.calculateTotalRewardsForGame(gameId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWins", totalWins);
        stats.put("totalRewardsDistributed", totalRewards != null ? totalRewards : BigDecimal.ZERO);
        
        return stats;
    }
}

