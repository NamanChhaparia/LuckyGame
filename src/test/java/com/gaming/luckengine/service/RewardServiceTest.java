package com.gaming.luckengine.service;

import com.gaming.luckengine.domain.entity.Game;
import com.gaming.luckengine.domain.entity.User;
import com.gaming.luckengine.domain.entity.Voucher;
import com.gaming.luckengine.domain.enums.GameStatus;
import com.gaming.luckengine.domain.enums.TransactionStatus;
import com.gaming.luckengine.dto.RewardRequest;
import com.gaming.luckengine.dto.RewardResponse;
import com.gaming.luckengine.repository.GameRepository;
import com.gaming.luckengine.repository.RewardTransactionRepository;
import com.gaming.luckengine.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private RewardTransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RewardService rewardService;

    private Game testGame;
    private Voucher testVoucher;
    private User testUser;

    @BeforeEach
    void setUp() {
        testGame = Game.builder()
                .id(1L)
                .gameCode("GAME_123")
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now().plusMinutes(5))
                .totalBudget(new BigDecimal("1000.00"))
                .remainingBudget(new BigDecimal("1000.00"))
                .status(GameStatus.ACTIVE)
                .winProbability(0.15)
                .volatilityFactor(1.2)
                .build();

        testVoucher = Voucher.builder()
                .id(1L)
                .voucherCode("NIKE50")
                .description("50% off Nike")
                .cost(new BigDecimal("5.00"))
                .currentQuantity(100)
                .isActive(true)
                .build();

        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .isActive(true)
                .build();
    }

    @Test
    void processBatch_GameNotActive_ReturnsAllLoss() {
        // Arrange
        testGame.setStatus(GameStatus.COMPLETED);
        RewardRequest request = RewardRequest.builder()
                .batchId("batch_1")
                .gameId(1L)
                .usernames(Arrays.asList("user1", "user2"))
                .build();

        when(transactionRepository.existsByBatchId("batch_1")).thenReturn(false);
        when(gameRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testGame));
        when(userService.getOrCreateUser(anyString(), any(), any())).thenReturn(testUser);
        when(transactionRepository.save(any())).thenReturn(null);

        // Act
        RewardResponse response = rewardService.processBatch(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getRewards().size());
        assertTrue(response.getRewards().stream()
                .allMatch(r -> r.getStatus() == TransactionStatus.LOSS));
        assertEquals(BigDecimal.ZERO, response.getTotalSpent());
    }

    @Test
    void processBatch_IdempotencyCheck_ReturnsCachedResults() {
        // Arrange
        RewardRequest request = RewardRequest.builder()
                .batchId("batch_1")
                .gameId(1L)
                .usernames(Arrays.asList("user1"))
                .build();

        when(transactionRepository.existsByBatchId("batch_1")).thenReturn(true);
        when(transactionRepository.findByBatchId("batch_1")).thenReturn(Arrays.asList());

        // Act
        RewardResponse response = rewardService.processBatch(request);

        // Assert
        assertNotNull(response);
        verify(gameRepository, never()).findByIdWithLock(any());
    }

    @Test
    void processBatch_ActiveGame_ProcessesSuccessfully() {
        // Arrange
        RewardRequest request = RewardRequest.builder()
                .batchId("batch_1")
                .gameId(1L)
                .usernames(Arrays.asList("user1", "user2", "user3"))
                .build();

        when(transactionRepository.existsByBatchId("batch_1")).thenReturn(false);
        when(gameRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testGame));
        when(voucherRepository.findAvailableVouchersWithinBudget(any()))
                .thenReturn(Arrays.asList(testVoucher));
        when(userService.getOrCreateUser(anyString(), any(), any())).thenReturn(testUser);
        when(transactionRepository.save(any())).thenReturn(null);

        // Act
        RewardResponse response = rewardService.processBatch(request);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getRewards().size());
        assertNotNull(response.getProcessingTimeMs());
        // Verify core interactions happened
        verify(gameRepository, atLeastOnce()).findByIdWithLock(1L);
        verify(transactionRepository, atLeast(3)).save(any());
        verify(userService, times(3)).getOrCreateUser(anyString(), any(), any());
    }

    @Test
    void processBatch_NoAvailableVouchers_ReturnsAllLoss() {
        // Arrange
        RewardRequest request = RewardRequest.builder()
                .batchId("batch_1")
                .gameId(1L)
                .usernames(Arrays.asList("user1"))
                .build();

        when(transactionRepository.existsByBatchId("batch_1")).thenReturn(false);
        when(gameRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testGame));
        when(voucherRepository.findAvailableVouchersWithinBudget(any()))
                .thenReturn(Arrays.asList());
        when(userService.getOrCreateUser(anyString(), any(), any())).thenReturn(testUser);
        when(transactionRepository.save(any())).thenReturn(null);

        // Act
        RewardResponse response = rewardService.processBatch(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getRewards().size());
        assertEquals(TransactionStatus.LOSS, response.getRewards().get(0).getStatus());
    }

    @Test
    void getGameStatistics_ReturnsCorrectStats() {
        // Arrange
        when(transactionRepository.countWinsByGame(1L)).thenReturn(50L);
        when(transactionRepository.calculateTotalRewardsForGame(1L))
                .thenReturn(new BigDecimal("250.00"));

        // Act
        Map<String, Object> stats = rewardService.getGameStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertEquals(50L, stats.get("totalWins"));
        assertEquals(new BigDecimal("250.00"), stats.get("totalRewardsDistributed"));
    }

    @Test
    void getUserTransactionHistory_ReturnsTransactions() {
        // Arrange
        when(transactionRepository.findByUserId(1L)).thenReturn(Arrays.asList());

        // Act
        List<?> history = rewardService.getUserTransactionHistory(1L);

        // Assert
        assertNotNull(history);
        verify(transactionRepository, times(1)).findByUserId(1L);
    }
}

