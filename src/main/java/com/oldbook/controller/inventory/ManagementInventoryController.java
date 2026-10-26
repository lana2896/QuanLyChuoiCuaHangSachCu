package com.oldbook.controller.inventory;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.inventory.ChainStockResponse;
import com.oldbook.dto.inventory.StockTransferResponse;
import com.oldbook.service.inventory.ChainInventoryService;
import com.oldbook.service.inventory.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Kho toàn chuỗi cho Quản lý / Quản trị viên (quyền do luật /api/management/** kiểm). */
@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
public class ManagementInventoryController {

    private final ChainInventoryService chainInventoryService;
    private final StockTransferService stockTransferService;

    @GetMapping("/inventory")
    public ApiResponse<List<ChainStockResponse>> inventory(@RequestParam(required = false) String tuKhoa) {
        return ApiResponse.success(chainInventoryService.search(tuKhoa));
    }

    @GetMapping("/transfers")
    public ApiResponse<List<StockTransferResponse>> transfers(@RequestParam(required = false) String trangThai) {
        return ApiResponse.success(stockTransferService.listForManagement(trangThai));
    }
}
