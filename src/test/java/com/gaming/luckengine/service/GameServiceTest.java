package com.gaming.luckengine.service;

import com.gaming.luckengine.domain.entity.Brand;
import com.gaming.luckengine.domain.entity.Game;
import com.gaming.luckengine.domain.enums.GameStatus;
import com.gaming.luckengine.exception.GameStateException;
import com.gaming.luckengine.exception.ResourceNotFoundException;
import com.gaming.luckengine.repository.GameBrandLinkRepository;
import com.gaming.luckengine.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameBrandLinkRepository gameBrandLinkRepository;

    @Mock
    private BrandService brandService;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private Brand testBrand;

    @BeforeEach
    void setUp() {
        testBrand = Brand.builder()
                .id(1L)
                .name("Nike")
                .walletBalance(new BigDecimal("10000.00"))
                .isActive(true)
                .build();

        testGame = Game.builder()
                .id(1L)
                .gameCode("GAME_123")
                .startTime(LocalDateTime.now().plusMinutes(5))
                .endTime(LocalDateTime.now().plusMinutes(15))
                .totalBudget(new BigDecimal("1000.00"))
                .remainingBudget(new BigDecimal("1000.00"))
                .status(GameStatus.SCHEDULED)
                .winProbability(0.15)
                .volatilityFactor(1.2)
                .build();
    }

    @Test
    void createGame_Success() {
        // Arrange
        Map<Long, BigDecimal> contributions = new HashMap<>();
        contributions.put(1L, new BigDecimal("1000.00"));
        when(brandService.getBrandById(1L)).thenReturn(testBrand);
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);
        when(brandService.deductFunds(any(), any())).thenReturn(testBrand);

        // Act
        Game result = gameService.createGame(
                LocalDateTime.now().plusMinutes(5),
                10,
                contributions,
                0.15
        );

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("1000.00"), result.getTotalBudget());
        assertEquals(GameStatus.SCHEDULED, result.getStatus());
        verify(gameRepository, times(1)).save(any(Game.class));
        verify(brandService, times(1)).deductFunds(1L, new BigDecimal("1000.00"));
    }

    @Test
    void createGame_InsufficientBrandFunds_ThrowsException() {
        // Arrange
        Map<Long, BigDecimal> contributions = new HashMap<>();
        contributions.put(1L, new BigDecimal("20000.00"));
        Brand poorBrand = Brand.builder()
                .id(1L)
                .name("Nike")
                .walletBalance(new BigDecimal("1000.00"))
                .build();
        when(brandService.getBrandById(1L)).thenReturn(poorBrand);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            gameService.createGame(
                    LocalDateTime.now().plusMinutes(5),
                    10,
                    contributions,
                    0.15
            );
        });
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void getGameById_Success() {
        // Arrange
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));

        // Act
        Game result = gameService.getGameById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getGameById_NotFound_ThrowsException() {
        // Arrange
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            gameService.getGameById(999L);
        });
    }

    @Test
    void startGame_Success() {
        // Arrange
        when(gameRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        // Act
        Game result = gameService.startGame(1L);

        // Assert
        assertNotNull(result);
        assertEquals(GameStatus.ACTIVE, result.getStatus());
        verify(gameRepository, times(1)).save(testGame);
    }

    @Test
    void startGame_InvalidStatus_ThrowsException() {
        // Arrange
        testGame.setStatus(GameStatus.COMPLETED);
        when(gameRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testGame));

        // Act & Assert
        assertThrows(GameStateException.class, () -> {
            gameService.startGame(1L);
        });
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void completeGame_Success() {
        // Arrange
        testGame.setStatus(GameStatus.ACTIVE);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        // Act
        Game result = gameService.completeGame(1L);

        // Assert
        assertNotNull(result);
        assertEquals(GameStatus.COMPLETED, result.getStatus());
        verify(gameRepository, times(1)).save(testGame);
    }

    @Test
    void deductGameBudget_Success() {
        // Arrange
        when(gameRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        // Act
        gameService.deductGameBudget(1L, new BigDecimal("500.00"));

        // Assert
        assertEquals(new BigDecimal("500.00"), testGame.getRemainingBudget());
        verify(gameRepository, times(1)).save(testGame);
    }
}

