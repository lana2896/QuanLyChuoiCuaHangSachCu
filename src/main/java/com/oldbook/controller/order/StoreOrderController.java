package com.oldbook.controller.order;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.order.CancelOrderRequest;
import com.oldbook.dto.order.SellerOrderResponse;
import com.oldbook.dto.order.StoreOrderProductOption;
import com.oldbook.security.store.StoreAccess;
import com.oldbook.service.order.StoreOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/store/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final StoreOrderService storeOrderService;
    private final StoreAccess storeAccess;

    @GetMapping
    public ApiResponse<List<SellerOrderResponse>> getMyStoreOrders(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String maDonHang,
            @RequestParam(required = false) Integer maSach,
            @RequestParam(required = false) String danhGia
    ) {
        Integer maND = storeAccess.requireOwner();
        return ApiResponse.success(storeOrderService.getMyStoreOrders(maND, trangThai, maDonHang, maSach, danhGia));
    }

    @GetMapping("/products")
    public ApiResponse<List<StoreOrderProductOption>> getOrderedProducts() {
        Integer maND = storeAccess.requireOwner();
        return ApiResponse.success(storeOrderService.getMyStoreOrderProducts(maND));
    }

    @PostMapping("/{maDHCH}/confirm")
    public ApiResponse<SellerOrderResponse> confirm(@PathVariable Integer maDHCH) {
        Integer maND = storeAccess.requireOwner();
        return ApiResponse.success(storeOrderService.confirmOrder(maND, maDHCH));
    }

    @PostMapping("/{maDHCH}/cancel")
    public ApiResponse<SellerOrderResponse> cancel(
            @PathVariable Integer maDHCH, @Valid @RequestBody CancelOrderRequest request) {
        Integer maND = storeAccess.requireOwner();
        return ApiResponse.success(storeOrderService.cancelOrder(maND, maDHCH, request.lyDo()));
    }

    @PostMapping("/{maDHCH}/cancel-request/approve")
    public ApiResponse<SellerOrderResponse> approveCancelRequest(@PathVariable Integer maDHCH) {
        Integer maND = storeAccess.requireOwner();
        return ApiResponse.success(storeOrderService.approveCancelRequest(maND, maDHCH));
    }

    @PostMapping("/{maDHCH}/cancel-request/reject")
    public ApiResponse<SellerOrderResponse> rejectCancelRequest(
            @PathVariable Integer maDHCH, @Valid @RequestBody CancelOrderRequest request) {
        Integer maND = storeAccess.requireOwner();
        return ApiResponse.success(storeOrderService.rejectCancelRequest(maND, maDHCH, request.lyDo()));
    }
}
