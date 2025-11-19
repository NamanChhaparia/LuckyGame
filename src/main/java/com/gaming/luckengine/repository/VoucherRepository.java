package com.gaming.luckengine.repository;

import com.gaming.luckengine.domain.entity.Voucher;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Voucher entity operations.
 */
@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    /**
     * Find voucher by code.
     */
    Optional<Voucher> findByVoucherCode(String voucherCode);

    /**
     * Find available vouchers for a given budget constraint.
     */
    @Query("SELECT v FROM Voucher v WHERE v.isActive = true " +
           "AND v.currentQuantity > 0 " +
           "AND v.cost <= :maxCost " +
           "AND (v.expiryDate IS NULL OR v.expiryDate > CURRENT_TIMESTAMP)")
    List<Voucher> findAvailableVouchersWithinBudget(@Param("maxCost") BigDecimal maxCost);

    /**
     * Find available vouchers for a specific brand.
     */
    @Query("SELECT v FROM Voucher v WHERE v.brand.id = :brandId " +
           "AND v.isActive = true AND v.currentQuantity > 0")
    List<Voucher> findAvailableVouchersByBrand(@Param("brandId") Long brandId);

    /**
     * Find voucher with pessimistic lock for inventory updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :voucherId")
    Optional<Voucher> findByIdWithLock(@Param("voucherId") Long voucherId);

    /**
     * Check if voucher code exists.
     */
    boolean existsByVoucherCode(String voucherCode);

    /**
     * Find all vouchers by brand.
     */
    List<Voucher> findByBrandId(Long brandId);
}

