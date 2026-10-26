package com.oldbook.controller.payment;

import com.oldbook.constant.auth.Role;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.payment.ApplyVoucherRequest;
import com.oldbook.dto.payment.VoucherOptionResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.payment.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping("/platform/available")
    public ResponseEntity<ApiResponse<List<VoucherOptionResponse>>> available(
            @Valid @RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                voucherService.listAvailable(getCustomerUserId(), request.getMaCTGioHangs())));
    }

    @PostMapping("/platform/apply")
    public ResponseEntity<ApiResponse<VoucherOptionResponse>> apply(
            @Valid @RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                voucherService.preview(getCustomerUserId(), request.getCode(), request.getMaCTGioHangs())));
    }

    @PostMapping("/shop/available")
    public ResponseEntity<ApiResponse<List<VoucherOptionResponse>>> availableShop(
            @Valid @RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                voucherService.listAvailableShop(getCustomerUserId(), request.getMaCTGioHangs())));
    }

    @PostMapping("/shop/apply")
    public ResponseEntity<ApiResponse<VoucherOptionResponse>> applyShop(
            @Valid @RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.previewShop(
                getCustomerUserId(), request.getCode(), request.getMaCH(), request.getMaCTGioHangs())));
    }

    private Integer getCustomerUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getDetails() instanceof AuthenticatedUserDetails user)) {
            throw new IllegalStateException("Thông tin xác thực không hợp lệ");
        }

        if (!Role.KHACH_HANG.name().equals(user.vaiTro())) {
            throw new AccessDeniedException("Chỉ khách hàng mới sử dụng được voucher");
        }

        return user.maND();
    }
}
