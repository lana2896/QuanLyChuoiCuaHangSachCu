package com.oldbook.controller.shipping;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.shipping.UpdateCarrierRequest;
import com.oldbook.dto.shipping.CarrierListResponse;
import com.oldbook.dto.shipping.ChangeCarrierStatusRequest;
import com.oldbook.dto.shipping.CarrierDetailResponse;
import com.oldbook.dto.shipping.CreateCarrierRequest;
import com.oldbook.service.shipping.CarrierManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/management/shipping-carriers")
@RequiredArgsConstructor
public class CarrierManagementController {

    private final CarrierManagementService service;

    @GetMapping
    public ResponseEntity<ApiResponse<CarrierListResponse>> getAll(
            @RequestParam(name = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(name = "trangThai", required = false) String trangThai) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(tuKhoa, trangThai, baseUrl())));
    }

    @GetMapping("/{maCode}")
    public ResponseEntity<ApiResponse<CarrierDetailResponse>> getDetail(@PathVariable("maCode") String maCode) {
        return ResponseEntity.ok(ApiResponse.success(service.getDetail(maCode, baseUrl())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CarrierDetailResponse>> create(
            @Valid @RequestBody CreateCarrierRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request, baseUrl())));
    }

    @PutMapping("/{maCode}")
    public ResponseEntity<ApiResponse<CarrierDetailResponse>> update(
            @PathVariable("maCode") String maCode,
            @Valid @RequestBody UpdateCarrierRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(maCode, request, baseUrl())));
    }

    @PatchMapping("/{maCode}/status")
    public ResponseEntity<ApiResponse<CarrierDetailResponse>> changeStatus(
            @PathVariable("maCode") String maCode,
            @Valid @RequestBody ChangeCarrierStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.changeStatus(maCode, request.getTrangThai(), request.getLyDo(), baseUrl())));
    }

    private static String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
