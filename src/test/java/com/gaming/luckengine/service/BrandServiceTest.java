package com.gaming.luckengine.service;

import com.gaming.luckengine.domain.entity.Brand;
import com.gaming.luckengine.exception.InsufficientFundsException;
import com.gaming.luckengine.exception.ResourceNotFoundException;
import com.gaming.luckengine.repository.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandService brandService;

    private Brand testBrand;

    @BeforeEach
    void setUp() {
        testBrand = Brand.builder()
                .id(1L)
                .name("Nike")
                .walletBalance(new BigDecimal("10000.00"))
                .dailySpendLimit(new BigDecimal("5000.00"))
                .isActive(true)
                .build();
    }

    @Test
    void createBrand_Success() {
        // Arrange
        when(brandRepository.existsByName("Nike")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenReturn(testBrand);

        // Act
        Brand result = brandService.createBrand("Nike", 
                new BigDecimal("10000.00"), 
                new BigDecimal("5000.00"));

        // Assert
        assertNotNull(result);
        assertEquals("Nike", result.getName());
        assertEquals(new BigDecimal("10000.00"), result.getWalletBalance());
        verify(brandRepository, times(1)).save(any(Brand.class));
    }

    @Test
    void createBrand_DuplicateName_ThrowsException() {
        // Arrange
        when(brandRepository.existsByName("Nike")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            brandService.createBrand("Nike", 
                    new BigDecimal("10000.00"), 
                    new BigDecimal("5000.00"));
        });
        verify(brandRepository, never()).save(any(Brand.class));
    }

    @Test
    void getBrandById_Success() {
        // Arrange
        when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

        // Act
        Brand result = brandService.getBrandById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Nike", result.getName());
    }

    @Test
    void getBrandById_NotFound_ThrowsException() {
        // Arrange
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            brandService.getBrandById(999L);
        });
    }

    @Test
    void depositFunds_Success() {
        // Arrange
        when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
        when(brandRepository.save(any(Brand.class))).thenReturn(testBrand);

        // Act
        Brand result = brandService.depositFunds(1L, new BigDecimal("5000.00"));

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("15000.00"), result.getWalletBalance());
        verify(brandRepository, times(1)).save(testBrand);
    }

    @Test
    void depositFunds_NegativeAmount_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            brandService.depositFunds(1L, new BigDecimal("-100.00"));
        });
    }

    @Test
    void deductFunds_Success() {
        // Arrange
        when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
        when(brandRepository.save(any(Brand.class))).thenReturn(testBrand);

        // Act
        Brand result = brandService.deductFunds(1L, new BigDecimal("3000.00"));

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("7000.00"), result.getWalletBalance());
        verify(brandRepository, times(1)).save(testBrand);
    }

    @Test
    void deductFunds_InsufficientBalance_ThrowsException() {
        // Arrange
        when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

        // Act & Assert
        assertThrows(InsufficientFundsException.class, () -> {
            brandService.deductFunds(1L, new BigDecimal("15000.00"));
        });
        verify(brandRepository, never()).save(any(Brand.class));
    }

    @Test
    void toggleBrandStatus_Success() {
        // Arrange
        when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
        when(brandRepository.save(any(Brand.class))).thenReturn(testBrand);

        // Act
        Brand result = brandService.toggleBrandStatus(1L, false);

        // Assert
        assertNotNull(result);
        assertFalse(result.getIsActive());
        verify(brandRepository, times(1)).save(testBrand);
    }
}

