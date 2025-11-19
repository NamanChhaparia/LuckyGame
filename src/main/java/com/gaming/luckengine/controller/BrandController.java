package com.gaming.luckengine.controller;

import com.gaming.luckengine.domain.entity.Brand;
import com.gaming.luckengine.dto.BrandCreateRequest;
import com.gaming.luckengine.service.BrandService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for Brand management operations.
 */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public ResponseEntity<Brand> createBrand(@Valid @RequestBody BrandCreateRequest request) {
        Brand brand = brandService.createBrand(
                request.getName(),
                request.getInitialBalance(),
                request.getDailySpendLimit()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(brand);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Brand> getBrandById(@PathVariable Long id) {
        Brand brand = brandService.getBrandById(id);
        return ResponseEntity.ok(brand);
    }

    @GetMapping
    public ResponseEntity<List<Brand>> getAllActiveBrands() {
        List<Brand> brands = brandService.getAllActiveBrands();
        return ResponseEntity.ok(brands);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Brand> depositFunds(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        Brand brand = brandService.depositFunds(id, amount);
        return ResponseEntity.ok(brand);
    }

    @PutMapping("/{id}/daily-limit")
    public ResponseEntity<Brand> updateDailyLimit(
            @PathVariable Long id,
            @RequestParam BigDecimal limit) {
        Brand brand = brandService.updateDailySpendLimit(id, limit);
        return ResponseEntity.ok(brand);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Brand> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        Brand brand = brandService.toggleBrandStatus(id, active);
        return ResponseEntity.ok(brand);
    }
}

