package com.oldbook.controller.inventory;

import com.oldbook.dto.branch.BranchOptionResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.inventory.CreateStockTransferRequest;
import com.oldbook.dto.inventory.StockTransferResponse;
import com.oldbook.dto.store.RejectRequest;
import com.oldbook.service.inventory.StockTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Điều chuyển sách của chi nhánh. Quyền Chủ chi nhánh do luật /api/store/** trong SecurityConfig kiểm. */
@RestController
@RequestMapping("/api/store/transfers")
@RequiredArgsConstructor
public class StoreTransferController {

    private final StockTransferService stockTransferService;

    @GetMapping
    public ApiResponse<List<StockTransferResponse>> list() {
        return ApiResponse.success(stockTransferService.listMine());
    }

    @GetMapping("/destinations")
    public ApiResponse<List<BranchOptionResponse>> destinations() {
        return ApiResponse.success(stockTransferService.destinations());
    }

    @PostMapping
    public ApiResponse<StockTransferResponse> create(@Valid @RequestBody CreateStockTransferRequest request) {
        return ApiResponse.success(stockTransferService.create(request));
    }

    @PostMapping("/{maPhieu}/accept")
    public ApiResponse<StockTransferResponse> accept(@PathVariable Integer maPhieu) {
        return ApiResponse.success(stockTransferService.accept(maPhieu));
    }

    @PostMapping("/{maPhieu}/reject")
    public ApiResponse<StockTransferResponse> reject(@PathVariable Integer maPhieu,
                                                     @Valid @RequestBody RejectRequest request) {
        return ApiResponse.success(stockTransferService.reject(maPhieu, request.lyDo()));
    }

    @PostMapping("/{maPhieu}/cancel")
    public ApiResponse<StockTransferResponse> cancel(@PathVariable Integer maPhieu) {
        return ApiResponse.success(stockTransferService.cancel(maPhieu));
    }
}
