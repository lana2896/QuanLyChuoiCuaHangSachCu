package com.oldbook.controller.shipping;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.order.SellerOrderResponse;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.security.store.StoreAccess;
import com.oldbook.service.order.StoreOrderService;
import com.oldbook.service.shipping.ShipmentTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cửa hàng chủ động kéo trạng thái vận đơn mới nhất (không chờ chu kỳ đồng bộ).
 * Tách riêng khỏi StoreOrderController để không sửa module order.
 */
@RestController
@RequestMapping("/api/store/orders")
@RequiredArgsConstructor
public class StoreShipmentSyncController {

    private final StoreOrderService storeOrderService;
    private final ShipmentTrackingService shipmentTrackingService;
    private final StoreAccess storeAccess;

    @PostMapping("/{maDHCH}/sync-shipping")
    public ApiResponse<SellerOrderResponse> syncShipping(@PathVariable Integer maDHCH) {
        Integer maND = storeAccess.requireOwner();
        findMyOrder(maND, maDHCH); // kiểm tra đơn thuộc cửa hàng của mình
        shipmentTrackingService.pullLatest(maDHCH);
        return ApiResponse.success(findMyOrder(maND, maDHCH));
    }

    private SellerOrderResponse findMyOrder(Integer maND, Integer maDHCH) {
        return storeOrderService.getMyStoreOrders(maND, null).stream()
                .filter(o -> o.getStoreOrder() != null && maDHCH.equals(o.getStoreOrder().getMaDHCH()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại"));
    }
}
