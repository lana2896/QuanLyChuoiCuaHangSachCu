package com.oldbook.controller.shipping;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.shipping.UpdateCarrierRequest;
import com.oldbook.dto.shipping.ChangeCarrierStatusRequest;
import com.oldbook.dto.shipping.CarrierResponse;
import com.oldbook.dto.shipping.CreateCarrierRequest;
import com.oldbook.service.shipping.CarrierManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Chỉ QUAN_LY / QUAN_TRI_VIEN (cấu hình trong SecurityConfig). */
@RestController
@RequestMapping("/api/management/shipping-carriers")
@RequiredArgsConstructor
public class CarrierManagementController {

    private final CarrierManagementService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarrierResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CarrierResponse>> create(@Valid @RequestBody CreateCarrierRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request)));
    }

    @PutMapping("/{maCode}")
    public ResponseEntity<ApiResponse<CarrierResponse>> update(
            @PathVariable String maCode,
            @Valid @RequestBody UpdateCarrierRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(maCode, request)));
    }

    @PatchMapping("/{maCode}/status")
    public ResponseEntity<ApiResponse<CarrierResponse>> changeStatus(
            @PathVariable String maCode,
            @Valid @RequestBody ChangeCarrierStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.changeStatus(maCode, request.getTrangThai())));
    }
}
