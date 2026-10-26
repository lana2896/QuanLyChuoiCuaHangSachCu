package com.oldbook.controller.payment;

import com.oldbook.constant.payment.TransferType;
import com.oldbook.constant.payment.TransferStatus;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.payment.BankTransactionResponse;
import com.oldbook.dto.payment.TransferRequestResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.payment.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/** Quản lý xử lý tiền sàn phải chuyển đi và xem sổ giao dịch ngân hàng. Quyền do luật /api/management/** kiểm. */
@RestController
@RequestMapping("/api/management/payments")
@RequiredArgsConstructor
public class PaymentManagementController {

    private final TransferService transferService;

    @GetMapping("/transfers")
    public ResponseEntity<ApiResponse<PageResponse<TransferRequestResponse>>> listTransfers(
            @RequestParam TransferType loai,
            @RequestParam(required = false) TransferStatus trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(transferService.listTransfers(loai, trangThai, page, size)));
    }

    /** Bấm sau khi đã chuyển khoản xong bằng tài khoản của sàn. */
    @PostMapping("/transfers/{maYeuCau}/done")
    public ResponseEntity<ApiResponse<TransferRequestResponse>> markTransferred(@PathVariable Integer maYeuCau) {
        return ResponseEntity.ok(ApiResponse.success(transferService.markTransferred(maTK(), maYeuCau)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<BankTransactionResponse>>> giaoDich(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(transferService.listTransactions(page, size)));
    }

    private static Integer maTK() {
        return ((AuthenticatedUserDetails) SecurityContextHolder.getContext().getAuthentication().getDetails()).maTK();
    }
}
