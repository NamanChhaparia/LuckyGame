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
        long startTime = System.currentTimeMillis();
        log.info("Processing batch: {} for game: {} with {} users", 
                request.getBatchId(), request.getGameId(), request.getUsernames().size());

        // Step 1: Idempotency check
        if (transactionRepository.existsByBatchId(request.getBatchId())) {
            log.warn("Batch {} already processed. Returning cached results.", request.getBatchId());
            return getCachedBatchResults(request.getBatchId());
        }

        // Step 2: Global checks - Game status and budget
        Game game = gameRepository.findByIdWithLock(request.getGameId())
                .orElseThrow(() -> new GameStateException("Game not found: " + request.getGameId()));

        if (!game.isActiveAndFunded()) {
            log.warn("Game {} is not active or has no budget. Returning all losses.", game.getId());
            return createAllLossResponse(request, game);
        }

        // Step 3: Calculate Tick Budget using Dynamic Decay Model
        BigDecimal tickBudget = game.calculateTickBudget();
        log.info("Tick budget calculated: {} (Remaining: {}, Seconds left: {})", 
                tickBudget, game.getRemainingBudget(), game.getRemainingSeconds());

        // Step 4: Fetch candidate vouchers within budget constraint
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

        for (String username : shuffledUsernames) {
            User user = userService.getOrCreateUser(username, null, null);
            
            UserRewardResult result = processUserReward(
                    user, game, request.getBatchId(), 
                    candidateVouchers, tickBudget, 
                    currentBatchSpend, game.getWinProbability());

            results.add(result);

            // Update current batch spend if user won
            if (result.getStatus() == TransactionStatus.WIN) {
                currentBatchSpend = currentBatchSpend.add(result.getAmount());
            }

            // Early termination if budget exhausted
            if (currentBatchSpend.compareTo(tickBudget) >= 0 || 
                currentBatchSpend.compareTo(game.getRemainingBudget()) >= 0) {
                log.info("Batch budget exhausted. Processing remaining users as losses.");
                // Process remaining users as losses
                for (int i = shuffledUsernames.indexOf(username) + 1; i < shuffledUsernames.size(); i++) {
                    User remainingUser = userService.getOrCreateUser(shuffledUsernames.get(i), null, null);
                    results.add(createLossResult(remainingUser, game, request.getBatchId()));
                }
                break;
            }
        }

        // Step 7: Update game budget
        if (currentBatchSpend.compareTo(BigDecimal.ZERO) > 0) {
            game.deductBudget(currentBatchSpend);
            gameRepository.save(game);
        }

        // Check if game budget is exhausted
        if (game.getRemainingBudget().compareTo(BigDecimal.ZERO) <= 0) {
            game.setStatus(GameStatus.BUDGET_EXHAUSTED);
            gameRepository.save(game);
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
     */
    private UserRewardResult processUserReward(User user, Game game, String batchId,
                                               List<Voucher> candidateVouchers,
                                               BigDecimal tickBudget,
                                               BigDecimal currentBatchSpend,
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

        for (Voucher voucher : shuffledVouchers) {
            // Constraint checks
            BigDecimal potentialSpend = currentBatchSpend.add(voucher.getCost());
            
            boolean withinTickBudget = potentialSpend.compareTo(tickBudget) <= 0;
            boolean withinGameBudget = potentialSpend.compareTo(game.getRemainingBudget()) <= 0;
            boolean hasInventory = voucher.isAvailable();

            if (withinTickBudget && withinGameBudget && hasInventory) {
                // All constraints pass - assign voucher
                try {
                    // Atomically decrement inventory
                    voucher.decrementInventory();
                    voucherRepository.save(voucher);

                    // Create win transaction
                    RewardTransaction transaction = RewardTransaction.builder()
                            .user(user)
                            .game(game)
                            .voucher(voucher)
                            .batchId(batchId)
                            .status(TransactionStatus.WIN)
                            .amount(voucher.getCost())
                            .rewardMessage("Congratulations! You won: " + voucher.getDescription())
                            .build();

                    transactionRepository.save(transaction);

                    return UserRewardResult.builder()
                            .username(user.getUsername())
                            .status(TransactionStatus.WIN)
                            .voucherId(voucher.getId())
                            .voucherCode(voucher.getVoucherCode())
                            .amount(voucher.getCost())
                            .message("Congratulations! You won: " + voucher.getDescription())
                            .build();

                } catch (Exception e) {
                    log.error("Error assigning voucher {} to user {}: {}", 
                            voucher.getId(), user.getUsername(), e.getMessage());
                    // Continue to next voucher
                }
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

