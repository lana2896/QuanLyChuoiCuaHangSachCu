package com.oldbook.controller.payment;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.payment.CreatePromotionRequest;
import com.oldbook.dto.payment.PromotionListResponse;
import com.oldbook.dto.payment.PromotionResponse;
import com.oldbook.dto.payment.PromotionStatusRequest;
import com.oldbook.dto.payment.UpdatePromotionRequest;
import com.oldbook.service.payment.PromotionManagementService;
import com.oldbook.service.store.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store/vouchers")
@RequiredArgsConstructor
public class StorePromotionController {

    private final PromotionManagementService service;
    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PromotionListResponse>> getAll(
            @RequestParam(name = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(name = "trangThai", required = false) String trangThai) {
        return ResponseEntity.ok(ApiResponse.success(service.getAllOfStore(currentStoreId(), tuKhoa, trangThai)));
    }

    @GetMapping("/{maKM}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getDetail(@PathVariable("maKM") Integer maKM) {
        return ResponseEntity.ok(ApiResponse.success(service.getDetailOfStore(currentStoreId(), maKM)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResponse>> create(
            @Valid @RequestBody CreatePromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.createInStore(currentStoreId(), request)));
    }

    @PutMapping("/{maKM}")
    public ResponseEntity<ApiResponse<PromotionResponse>> update(
            @PathVariable("maKM") Integer maKM,
            @Valid @RequestBody UpdatePromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateInStore(currentStoreId(), maKM, request)));
    }

    @PatchMapping("/{maKM}/status")
    public ResponseEntity<ApiResponse<PromotionResponse>> changeStatus(
            @PathVariable("maKM") Integer maKM,
            @Valid @RequestBody PromotionStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.changeStatusInStore(currentStoreId(), maKM, request.getTrangThai())));
    }

    @DeleteMapping("/{maKM}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("maKM") Integer maKM) {
        service.deleteInStore(currentStoreId(), maKM);
        return ResponseEntity.ok(ApiResponse.<Void>success("Đã xóa voucher"));
    }

    // Việc xác định chi nhánh của chủ cửa hàng thuộc lớp nghiệp vụ (StoreService), controller chỉ gọi lại.
    private Integer currentStoreId() {
        return storeService.getCurrentStoreId();
    }
}
