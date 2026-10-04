package com.oldbook.controller.cart;

import com.oldbook.constant.auth.VaiTro;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.cart.AddCartItemRequest;
import com.oldbook.dto.cart.CartResponse;
import com.oldbook.dto.cart.UpdateCartItemRequest;
import com.oldbook.service.cart.GioHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final GioHangService gioHangService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {

        Integer maND = getMaNDKhachHang();

        return ResponseEntity.ok(
                ApiResponse.success(gioHangService.getCart(maND))
        );
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddCartItemRequest request
    ) {

        Integer maND = getMaNDKhachHang();

        return ResponseEntity.ok(
                ok(
                        "Đã thêm sách vào giỏ hàng",
                        gioHangService.addItem(maND, request)
                )
        );
    }

    @PutMapping("/items/{maCTGioHang}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Integer maCTGioHang,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {

        Integer maND = getMaNDKhachHang();

        return ResponseEntity.ok(
                ok(
                        "Đã cập nhật số lượng",
                        gioHangService.updateItem(maND, maCTGioHang, request)
                )
        );
    }

    @DeleteMapping("/items/{maCTGioHang}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Integer maCTGioHang
    ) {

        Integer maND = getMaNDKhachHang();

        return ResponseEntity.ok(
                ok(
                        "Đã xóa sách khỏi giỏ hàng",
                        gioHangService.removeItem(maND, maCTGioHang)
                )
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart() {

        Integer maND = getMaNDKhachHang();

        return ResponseEntity.ok(
                ok(
                        "Đã xóa toàn bộ giỏ hàng",
                        gioHangService.clearCart(maND)
                )
        );
    }

    private Integer getMaNDKhachHang() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getDetails() instanceof AuthenticatedUserDetails user)) {

            throw new IllegalStateException("Thông tin xác thực không hợp lệ");
        }

        if (!VaiTro.KHACH_HANG.name().equals(user.vaiTro())) {
            throw new AccessDeniedException(
                    "Chỉ khách hàng mới sử dụng được giỏ hàng"
            );
        }

        return user.maND();
    }

    private <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }
}
