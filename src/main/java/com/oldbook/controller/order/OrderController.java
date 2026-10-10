package com.oldbook.controller.order;

import com.oldbook.constant.auth.Role;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.order.CreateOrderRequest;
import com.oldbook.dto.order.OrderResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.order.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        Integer maND = getCustomerUserId();

        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .code(200)
                        .message("Đặt hàng thành công")
                        .data(orderService.createOrder(maND, request))
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders() {
        Integer maND = getCustomerUserId();

        return ResponseEntity.ok(
                ApiResponse.success(orderService.getMyOrders(maND))
        );
    }

    @GetMapping("/{maDH}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(
            @PathVariable Integer maDH
    ) {
        Integer maND = getCustomerUserId();

        return ResponseEntity.ok(
                ApiResponse.success(orderService.getMyOrder(maND, maDH))
        );
    }

    private Integer getCustomerUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getDetails() instanceof AuthenticatedUserDetails user)) {
            throw new IllegalStateException("Thông tin xác thực không hợp lệ");
        }

        if (!Role.KHACH_HANG.name().equals(user.vaiTro())) {
            throw new AccessDeniedException(
                    "Chỉ khách hàng mới được đặt hàng"
            );
        }

        return user.maND();
    }
}
