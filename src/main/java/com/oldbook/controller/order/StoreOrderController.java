package com.oldbook.controller.order;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.order.SellerOrderResponse;
import com.oldbook.security.store.StoreAccess;
import com.oldbook.service.order.StoreOrderService;
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
            @RequestParam(required = false) String trangThai
    ) {
        Integer maND = storeAccess.requireOwner();
        return ApiResponse.success(storeOrderService.getMyStoreOrders(maND, trangThai));
    }

    @PostMapping("/{maDHCH}/confirm")
    public ApiResponse<SellerOrderResponse> confirm(@PathVariable Integer maDHCH) {
        Integer maND = storeAccess.requireOwner();
        return ApiResponse.success(storeOrderService.confirmOrder(maND, maDHCH));
    }
}
