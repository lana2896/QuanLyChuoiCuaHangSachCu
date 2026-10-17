package com.oldbook.controller.shipping;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.shipping.UpdateShipmentRequest;
import com.oldbook.dto.shipping.CarrierResponse;
import com.oldbook.dto.shipping.AcceptShipmentRequest;
import com.oldbook.dto.shipping.ShipmentDetailResponse;
import com.oldbook.dto.shipping.ShipmentRequestResponse;
import com.oldbook.service.shipping.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API cho đơn vị vận chuyển, không cần đăng nhập. Mỗi hãng có bộ API riêng theo mã:
 *   /api/shipping/GHN/...   /api/shipping/JT/...
 * Thêm hãng mới (qua /api/management/shipping-carriers) là có ngay bộ API này, không phải sửa code.
 */
@RestController
@RequestMapping("/api/shipping/{maCode}")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentService shipmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<CarrierResponse>> getCarrier(@PathVariable String maCode) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.getCarrier(maCode)));
    }

    // ===== 1. Tiếp nhận yêu cầu vận chuyển =====

    @GetMapping("/requests/pending")
    public ResponseEntity<ApiResponse<List<ShipmentRequestResponse>>> getPendingRequests(@PathVariable String maCode) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.getPendingRequests(maCode)));
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<ShipmentDetailResponse>> acceptShipment(
            @PathVariable String maCode,
            @Valid @RequestBody AcceptShipmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.acceptShipment(maCode, request)));
    }

    // ===== 2. Cập nhật trạng thái vận chuyển và kết quả giao hàng, hoàn trả =====

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<ShipmentDetailResponse>>> getAll(@PathVariable String maCode) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.getAll(maCode)));
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<ApiResponse<ShipmentDetailResponse>> getById(
            @PathVariable String maCode,
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.getById(maCode, id)));
    }

    @PatchMapping("/requests/{id}/status")
    public ResponseEntity<ApiResponse<ShipmentDetailResponse>> updateStatus(
            @PathVariable String maCode,
            @PathVariable Integer id,
            @Valid @RequestBody UpdateShipmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.updateStatus(maCode, id, request)));
    }

    @PatchMapping("/requests/{id}/result")
    public ResponseEntity<ApiResponse<ShipmentDetailResponse>> updateResult(
            @PathVariable String maCode,
            @PathVariable Integer id,
            @Valid @RequestBody UpdateShipmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.updateResult(maCode, id, request)));
    }
}
