package com.gaming.luckengine.service;

import com.gaming.luckengine.domain.entity.Brand;
import com.gaming.luckengine.exception.ResourceNotFoundException;
import com.gaming.luckengine.exception.InsufficientFundsException;
import com.gaming.luckengine.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for managing Brand operations.
 * Handles brand creation, wallet management, and budget operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrandService {

    private final BrandRepository brandRepository;

    /**
     * Create a new brand.
     */
    @Transactional
    public Brand createBrand(String name, BigDecimal initialBalance, BigDecimal dailySpendLimit) {
        log.info("Creating brand: {} with initial balance: {}", name, initialBalance);
        
        if (brandRepository.existsByName(name)) {
            throw new IllegalArgumentException("Brand with name already exists: " + name);
        }

        Brand brand = Brand.builder()
                .name(name)
                .walletBalance(initialBalance)
                .dailySpendLimit(dailySpendLimit)
                .isActive(true)
                .build();

        return brandRepository.save(brand);
    }

    /**
     * Get brand by ID.
     */
    @Transactional(readOnly = true)
    public Brand getBrandById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }

    /**
     * Get brand by name.
     */
    @Transactional(readOnly = true)
    public Brand getBrandByName(String name) {
        return brandRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with name: " + name));
    }

    /**
     * Get all active brands.
     */
    @Transactional(readOnly = true)
    public List<Brand> getAllActiveBrands() {
        return brandRepository.findByIsActiveTrue();
    }

    /**
     * Add funds to brand wallet.
     */
    @Transactional
    public Brand depositFunds(Long brandId, BigDecimal amount) {
        log.info("Depositing {} to brand {}", amount, brandId);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        Brand brand = getBrandById(brandId);
        brand.addToWallet(amount);
        
        return brandRepository.save(brand);
    }

    /**
     * Deduct funds from brand wallet.
     * Used when locking budget for games.
     */
    @Transactional
    public Brand deductFunds(Long brandId, BigDecimal amount) {
        log.info("Deducting {} from brand {}", amount, brandId);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduction amount must be positive");
        }

        Brand brand = getBrandById(brandId);
        
        if (!brand.canAfford(amount)) {
            throw new InsufficientFundsException(
                String.format("Insufficient funds in brand wallet. Available: %s, Required: %s",
                    brand.getWalletBalance(), amount));
        }
        
        brand.deductFromWallet(amount);
        
        return brandRepository.save(brand);
    }

    /**
     * Update brand daily spend limit.
     */
    @Transactional
    public Brand updateDailySpendLimit(Long brandId, BigDecimal newLimit) {
        Brand brand = getBrandById(brandId);
        brand.setDailySpendLimit(newLimit);
        return brandRepository.save(brand);
    }

    /**
     * Activate/Deactivate brand.
     */
    @Transactional
    public Brand toggleBrandStatus(Long brandId, boolean isActive) {
        Brand brand = getBrandById(brandId);
        brand.setIsActive(isActive);
        return brandRepository.save(brand);
    }

    /**
     * Get brands with minimum balance.
     */
    @Transactional(readOnly = true)
    public List<Brand> getBrandsWithMinimumBalance(BigDecimal minBalance) {
        return brandRepository.findBrandsWithMinBalance(minBalance);
    }
}

