package com.gaming.luckengine.repository;

import com.gaming.luckengine.domain.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Brand entity operations.
 */
@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    /**
     * Find brand by name.
     */
    Optional<Brand> findByName(String name);

    /**
     * Find all active brands.
     */
    List<Brand> findByIsActiveTrue();

    /**
     * Find brands with sufficient wallet balance.
     */
    @Query("SELECT b FROM Brand b WHERE b.isActive = true AND b.walletBalance >= :minBalance")
    List<Brand> findBrandsWithMinBalance(java.math.BigDecimal minBalance);

    /**
     * Check if brand exists by name.
     */
    boolean existsByName(String name);
}

