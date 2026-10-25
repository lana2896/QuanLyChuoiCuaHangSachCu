package com.oldbook.controller.catalog;

import com.oldbook.dto.catalog.BookTitleRequest;
import com.oldbook.dto.catalog.BookTitleResponse;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.catalog.UpdateCatalogStatusRequest;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.store.BookImageUploadResponse;
import com.oldbook.service.media.ImageUploadService;
import com.oldbook.service.catalog.BookTitleManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/management/book-titles")
@RequiredArgsConstructor
public class BookTitleManagementController {

    private final BookTitleManagementService service;
    private final ImageUploadService imageUploadService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BookImageUploadResponse>> uploadImage(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                new BookImageUploadResponse(imageUploadService.upload(file, "oldbook/books"))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookTitleResponse>>> search(
            @RequestParam(name = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(name = "maDM", required = false) Integer maDM,
            @RequestParam(name = "trangThaiBan", required = false) String trangThaiBan,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.search(tuKhoa, maDM, trangThaiBan, page, size)));
    }

    @GetMapping("/{maTuaSach}")
    public ResponseEntity<ApiResponse<BookTitleResponse>> get(@PathVariable("maTuaSach") Integer maTuaSach) {
        return ResponseEntity.ok(ApiResponse.success(service.get(maTuaSach)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookTitleResponse>> create(@Valid @RequestBody BookTitleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request)));
    }

    @PutMapping("/{maTuaSach}")
    public ResponseEntity<ApiResponse<BookTitleResponse>> update(
            @PathVariable("maTuaSach") Integer maTuaSach, @Valid @RequestBody BookTitleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(maTuaSach, request)));
    }

    @PatchMapping("/{maTuaSach}/sale-status")
    public ResponseEntity<ApiResponse<BookTitleResponse>> updateSaleStatus(
            @PathVariable("maTuaSach") Integer maTuaSach, @Valid @RequestBody UpdateCatalogStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateSaleStatus(maTuaSach, request)));
    }
}
