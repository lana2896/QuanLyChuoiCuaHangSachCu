package com.oldbook.controller.catalog;

import com.oldbook.dto.catalog.CategoryRequest;
import com.oldbook.dto.catalog.CategoryResponse;
import com.oldbook.dto.catalog.UpdateCatalogStatusRequest;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.service.catalog.CategoryManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/management/categories")
@RequiredArgsConstructor
public class CategoryManagementController {

    private final CategoryManagementService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request)));
    }

    @PutMapping("/{maDM}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable("maDM") Integer maDM, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(maDM, request)));
    }

    @PatchMapping("/{maDM}/sale-status")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateSaleStatus(
            @PathVariable("maDM") Integer maDM, @Valid @RequestBody UpdateCatalogStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateSaleStatus(maDM, request)));
    }
}
