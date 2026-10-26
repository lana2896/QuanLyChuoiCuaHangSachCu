package com.oldbook.controller.payment;

import com.oldbook.constant.auth.Role;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.order.CreateOrderRequest;
import com.oldbook.dto.payment.PaymentConfigResponse;
import com.oldbook.dto.payment.PaymentSessionResponse;
import com.oldbook.dto.payment.BankAccountRequest;
import com.oldbook.dto.payment.TransferRequestResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.payment.TransferService;
import com.oldbook.service.payment.PaymentSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Thanh toán chuyển khoản phía khách hàng: phiên thanh toán và các khoản hoàn tiền của chính họ. */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentSessionService paymentSessionService;
    private final TransferService transferService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<PaymentConfigResponse>> config() {
        return ResponseEntity.ok(ApiResponse.success(paymentSessionService.getConfig()));
    }

    /** Nhận đúng nội dung của yêu cầu đặt hàng; đơn chỉ được tạo khi tiền về. */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<PaymentSessionResponse>> createSession(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentSessionService.createSession(maNDKhachHang(), request)));
    }

    @GetMapping("/sessions/{maPhien}")
    public ResponseEntity<ApiResponse<PaymentSessionResponse>> getSession(@PathVariable Integer maPhien) {
        return ResponseEntity.ok(ApiResponse.success(paymentSessionService.getSession(maNDKhachHang(), maPhien)));
    }

    @DeleteMapping("/sessions/{maPhien}")
    public ResponseEntity<ApiResponse<PaymentSessionResponse>> cancelSession(@PathVariable Integer maPhien) {
        return ResponseEntity.ok(ApiResponse.success(paymentSessionService.cancelSession(maNDKhachHang(), maPhien)));
    }

    @GetMapping("/refunds")
    public ResponseEntity<ApiResponse<List<TransferRequestResponse>>> myRefunds() {
        return ResponseEntity.ok(ApiResponse.success(transferService.myRefunds(maNDKhachHang())));
    }

    @PutMapping("/refunds/{maYeuCau}/account")
    public ResponseEntity<ApiResponse<TransferRequestResponse>> submitRefundAccount(
            @PathVariable Integer maYeuCau,
            @Valid @RequestBody BankAccountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                transferService.submitRefundAccount(maNDKhachHang(), maYeuCau, request)
        ));
    }

    private Integer maNDKhachHang() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getDetails() instanceof AuthenticatedUserDetails user)) {
            throw new IllegalStateException("Thông tin xác thực không hợp lệ");
        }
        if (!Role.KHACH_HANG.name().equals(user.vaiTro())) {
            throw new AccessDeniedException("Chỉ khách hàng mới dùng được chức năng thanh toán");
        }
        return user.maND();
    }
}
