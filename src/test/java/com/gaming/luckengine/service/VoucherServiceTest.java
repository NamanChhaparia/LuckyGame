package com.gaming.luckengine.service;

import com.gaming.luckengine.domain.entity.Brand;
import com.gaming.luckengine.domain.entity.Voucher;
import com.gaming.luckengine.exception.InsufficientInventoryException;
import com.gaming.luckengine.exception.ResourceNotFoundException;
import com.gaming.luckengine.repository.VoucherRepository;
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
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private BrandService brandService;

    @InjectMocks
    private VoucherService voucherService;

    private Voucher testVoucher;
    private Brand testBrand;

    @BeforeEach
    void setUp() {
        testBrand = Brand.builder()
                .id(1L)
                .name("Nike")
                .walletBalance(new BigDecimal("10000.00"))
                .build();

        testVoucher = Voucher.builder()
                .id(1L)
                .voucherCode("NIKE50")
                .description("50% off")
                .cost(new BigDecimal("5.00"))
                .initialQuantity(100)
                .currentQuantity(100)
                .brand(testBrand)
                .isActive(true)
                .build();
    }

    @Test
    void createVoucher_Success() {
        // Arrange
        when(voucherRepository.existsByVoucherCode("NIKE50")).thenReturn(false);
        when(brandService.getBrandById(1L)).thenReturn(testBrand);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(testVoucher);

        // Act
        Voucher result = voucherService.createVoucher(
                1L, "NIKE50", "50% off", 
                new BigDecimal("5.00"), 100, null);

        // Assert
        assertNotNull(result);
        assertEquals("NIKE50", result.getVoucherCode());
        verify(voucherRepository, times(1)).save(any(Voucher.class));
    }

    @Test
    void createVoucher_DuplicateCode_ThrowsException() {
        // Arrange
        when(voucherRepository.existsByVoucherCode("NIKE50")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            voucherService.createVoucher(
                    1L, "NIKE50", "50% off", 
                    new BigDecimal("5.00"), 100, null);
        });
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void createVoucher_BrandCannotAfford_ThrowsException() {
        // Arrange
        Brand poorBrand = Brand.builder()
                .id(1L)
                .name("Nike")
                .walletBalance(new BigDecimal("100.00"))
                .build();

        when(voucherRepository.existsByVoucherCode("NIKE50")).thenReturn(false);
        when(brandService.getBrandById(1L)).thenReturn(poorBrand);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            voucherService.createVoucher(
                    1L, "NIKE50", "50% off", 
                    new BigDecimal("5.00"), 1000, null);
        });
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void getVoucherById_Success() {
        // Arrange
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(testVoucher));

        // Act
        Voucher result = voucherService.getVoucherById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getVoucherById_NotFound_ThrowsException() {
        // Arrange
        when(voucherRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            voucherService.getVoucherById(999L);
        });
    }

    @Test
    void decrementInventory_Success() {
        // Arrange
        when(voucherRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(testVoucher);

        // Act
        Voucher result = voucherService.decrementInventory(1L);

        // Assert
        assertNotNull(result);
        assertEquals(99, result.getCurrentQuantity());
        verify(voucherRepository, times(1)).save(testVoucher);
    }

    @Test
    void decrementInventory_NoInventory_ThrowsException() {
        // Arrange
        testVoucher.setCurrentQuantity(0);
        when(voucherRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testVoucher));

        // Act & Assert
        assertThrows(InsufficientInventoryException.class, () -> {
            voucherService.decrementInventory(1L);
        });
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void addInventory_Success() {
        // Arrange
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(testVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(testVoucher);

        // Act
        Voucher result = voucherService.addInventory(1L, 50);

        // Assert
        assertNotNull(result);
        assertEquals(150, result.getCurrentQuantity());
        verify(voucherRepository, times(1)).save(testVoucher);
    }

    @Test
    void isVoucherAvailable_ReturnsTrue() {
        // Arrange
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(testVoucher));

        // Act
        boolean result = voucherService.isVoucherAvailable(1L);

        // Assert
        assertTrue(result);
    }

    @Test
    void isVoucherAvailable_NoInventory_ReturnsFalse() {
        // Arrange
        testVoucher.setCurrentQuantity(0);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(testVoucher));

        // Act
        boolean result = voucherService.isVoucherAvailable(1L);

        // Assert
        assertFalse(result);
    }
}

