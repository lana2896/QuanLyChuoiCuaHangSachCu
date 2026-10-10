package com.oldbook.controller.store;

import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.store.CreateBookRequest;
import com.oldbook.dto.store.UpdateBookRequest;
import com.oldbook.dto.store.UpdateStockRequest;
import com.oldbook.dto.store.UpdateSaleStatusRequest;
import com.oldbook.service.store.StoreBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store/me/books")
@RequiredArgsConstructor
public class StoreBookController {
    private final StoreBookService storeBookService;

    @GetMapping
    public ApiResponse<PageResponse<BookResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ngayTao,desc") String sort,
            @RequestParam(required = false) String trangThaiDuyet) {
        return ApiResponse.success(storeBookService.listMyBooks(page, size, sort, trangThaiDuyet));
    }

    @GetMapping("/{maSach}")
    public ApiResponse<BookResponse> get(@PathVariable Integer maSach) {
        return ApiResponse.success(storeBookService.getMyBook(maSach));
    }

    @PostMapping
    public ApiResponse<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        return ApiResponse.success(storeBookService.create(request));
    }

    @PutMapping("/{maSach}")
    public ApiResponse<BookResponse> update(@PathVariable Integer maSach,
            @Valid @RequestBody UpdateBookRequest request) {
        return ApiResponse.success(storeBookService.update(maSach, request));
    }

    @PatchMapping("/{maSach}/stock")
    public ApiResponse<BookResponse> updateStock(@PathVariable Integer maSach,
            @Valid @RequestBody UpdateStockRequest request) {
        return ApiResponse.success(storeBookService.updateStock(maSach, request));
    }

    @PatchMapping("/{maSach}/sale-status")
    public ApiResponse<BookResponse> updateSaleStatus(@PathVariable Integer maSach,
            @Valid @RequestBody UpdateSaleStatusRequest request) {
        return ApiResponse.success(storeBookService.updateSaleStatus(maSach, request));
    }
}
