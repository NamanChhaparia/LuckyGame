package com.gaming.luckengine.service;

import com.gaming.luckengine.domain.entity.Brand;
import com.gaming.luckengine.domain.entity.Voucher;
import com.gaming.luckengine.exception.InsufficientInventoryException;
import com.gaming.luckengine.exception.ResourceNotFoundException;
import com.gaming.luckengine.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing Voucher operations.
 * Handles voucher creation, inventory management, and availability checks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final BrandService brandService;

    /**
     * Create a new voucher.
     */
    @Transactional
    public Voucher createVoucher(Long brandId, String code, String description, 
                                  BigDecimal cost, Integer quantity, LocalDateTime expiryDate) {
        log.info("Creating voucher: {} for brand: {}", code, brandId);
        
        if (voucherRepository.existsByVoucherCode(code)) {
            throw new IllegalArgumentException("Voucher code already exists: " + code);
        }

        Brand brand = brandService.getBrandById(brandId);
        
        // Validate that brand can afford the total voucher value
        BigDecimal totalValue = cost.multiply(BigDecimal.valueOf(quantity));
        if (!brand.canAfford(totalValue)) {
            throw new IllegalArgumentException(
                String.format("Brand cannot afford voucher inventory. Total value: %s, Available: %s",
                    totalValue, brand.getWalletBalance()));
        }

        Voucher voucher = Voucher.builder()
                .voucherCode(code)
                .description(description)
                .cost(cost)
                .initialQuantity(quantity)
                .currentQuantity(quantity)
                .brand(brand)
                .expiryDate(expiryDate)
                .isActive(true)
                .build();

        return voucherRepository.save(voucher);
    }

    /**
     * Get voucher by ID.
     */
    @Transactional(readOnly = true)
    public Voucher getVoucherById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));
    }

    /**
     * Get available vouchers within budget constraint.
     */
    @Transactional(readOnly = true)
    public List<Voucher> getAvailableVouchersWithinBudget(BigDecimal maxBudget) {
        return voucherRepository.findAvailableVouchersWithinBudget(maxBudget);
    }

    /**
     * Get available vouchers for a specific brand.
     */
    @Transactional(readOnly = true)
    public List<Voucher> getAvailableVouchersByBrand(Long brandId) {
        return voucherRepository.findAvailableVouchersByBrand(brandId);
    }

    /**
     * Decrement voucher inventory atomically.
     * Uses pessimistic locking to prevent race conditions.
     */
    @Transactional
    public Voucher decrementInventory(Long voucherId) {
        log.debug("Decrementing inventory for voucher: {}", voucherId);
        
        Voucher voucher = voucherRepository.findByIdWithLock(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + voucherId));

        if (!voucher.isAvailable()) {
            throw new InsufficientInventoryException("Voucher is not available: " + voucherId);
        }

        voucher.decrementInventory();
        
        return voucherRepository.save(voucher);
    }

    /**
     * Check if voucher is available.
     */
    @Transactional(readOnly = true)
    public boolean isVoucherAvailable(Long voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        return voucher.isAvailable();
    }

    /**
     * Update voucher status.
     */
    @Transactional
    public Voucher updateVoucherStatus(Long voucherId, boolean isActive) {
        Voucher voucher = getVoucherById(voucherId);
        voucher.setIsActive(isActive);
        return voucherRepository.save(voucher);
    }

    /**
     * Add inventory to existing voucher.
     */
    @Transactional
    public Voucher addInventory(Long voucherId, Integer quantity) {
        log.info("Adding {} inventory to voucher: {}", quantity, voucherId);
        
        Voucher voucher = getVoucherById(voucherId);
        voucher.setCurrentQuantity(voucher.getCurrentQuantity() + quantity);
        voucher.setInitialQuantity(voucher.getInitialQuantity() + quantity);
        
        return voucherRepository.save(voucher);
    }

    /**
     * Get all vouchers for a brand.
     */
    @Transactional(readOnly = true)
    public List<Voucher> getVouchersByBrand(Long brandId) {
        return voucherRepository.findByBrandId(brandId);
    }
}

