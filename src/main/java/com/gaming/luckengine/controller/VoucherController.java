package com.gaming.luckengine.controller;

import com.gaming.luckengine.domain.entity.Voucher;
import com.gaming.luckengine.dto.VoucherCreateRequest;
import com.gaming.luckengine.service.VoucherService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for Voucher management operations.
 */
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    public ResponseEntity<Voucher> createVoucher(@Valid @RequestBody VoucherCreateRequest request) {
        Voucher voucher = voucherService.createVoucher(
                request.getBrandId(),
                request.getVoucherCode(),
                request.getDescription(),
                request.getCost(),
                request.getQuantity(),
                request.getExpiryDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(voucher);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Voucher> getVoucherById(@PathVariable Long id) {
        Voucher voucher = voucherService.getVoucherById(id);
        return ResponseEntity.ok(voucher);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Voucher>> getAvailableVouchers(
            @RequestParam(required = false) BigDecimal maxBudget) {
        List<Voucher> vouchers = maxBudget != null
                ? voucherService.getAvailableVouchersWithinBudget(maxBudget)
                : voucherService.getAvailableVouchersByBrand(null);
        return ResponseEntity.ok(vouchers);
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<List<Voucher>> getVouchersByBrand(@PathVariable Long brandId) {
        List<Voucher> vouchers = voucherService.getVouchersByBrand(brandId);
        return ResponseEntity.ok(vouchers);
    }

    @PostMapping("/{id}/add-inventory")
    public ResponseEntity<Voucher> addInventory(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        Voucher voucher = voucherService.addInventory(id, quantity);
        return ResponseEntity.ok(voucher);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Voucher> updateStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        Voucher voucher = voucherService.updateVoucherStatus(id, active);
        return ResponseEntity.ok(voucher);
    }
}

