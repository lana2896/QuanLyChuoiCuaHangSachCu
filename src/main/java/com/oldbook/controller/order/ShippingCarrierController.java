package com.oldbook.controller.order;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.shipping.ShippingCarrierResponse;
import com.oldbook.service.order.ShippingCarrierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shipping-carriers")
@RequiredArgsConstructor
public class ShippingCarrierController {

    private final ShippingCarrierService shippingCarrierService;

    @GetMapping
    public ApiResponse<List<ShippingCarrierResponse>> getCarriers() {
        return ApiResponse.success(shippingCarrierService.getActiveCarriers());
    }
}
