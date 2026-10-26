package com.oldbook.controller.payment;

import com.oldbook.constant.payment.TransferType;
import com.oldbook.constant.payment.TransferStatus;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.constant.payment.ReconciliationResult;
import com.oldbook.dto.management.ManagementDto.GiaoDichTrang;
import com.oldbook.dto.payment.TransferRequestResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.payment.TransferService;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Quản lý xử lý tiền trụ sở phải chuyển đi và xem sổ giao dịch ngân hàng. Quyền do luật /api/management/** kiểm. */
@RestController
@RequestMapping("/api/management/payments")
@RequiredArgsConstructor
public class PaymentManagementController {

    private final TransferService transferService;

    @GetMapping("/transfers")
    public ResponseEntity<ApiResponse<PageResponse<TransferRequestResponse>>> listTransfers(
            @RequestParam TransferType loai,
            @RequestParam(required = false) TransferStatus trangThai,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(transferService.listTransfers(loai, trangThai, tuKhoa, page, size)));
    }

    /** Bấm sau khi đã chuyển khoản xong bằng tài khoản của trụ sở. */
    @PostMapping("/transfers/{maYeuCau}/done")
    public ResponseEntity<ApiResponse<TransferRequestResponse>> markTransferred(@PathVariable Integer maYeuCau) {
        return ResponseEntity.ok(ApiResponse.success(transferService.markTransferred(maTK(), maYeuCau)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<GiaoDichTrang>> giaoDich(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) ReconciliationResult ketQua,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                transferService.listTransactions(tuKhoa, ketQua, tuNgay, denNgay, page, size)));
    }

    private static Integer maTK() {
        return ((AuthenticatedUserDetails) SecurityContextHolder.getContext().getAuthentication().getDetails()).maTK();
    }
}
