package com.oldbook.controller.store;

import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.store.ModerationSearchRequest;
import com.oldbook.dto.store.RejectRequest;
import com.oldbook.dto.store.StoreResponse;
import com.oldbook.service.store.StoreModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store/moderation")
@RequiredArgsConstructor
public class StoreModerationController {

    private final StoreModerationService storeModerationService;

    @GetMapping("/stores")
    public ApiResponse<PageResponse<StoreResponse>> searchStores(
            @Valid @ModelAttribute ModerationSearchRequest request) {
        return ApiResponse.success(storeModerationService.searchStores(request));
    }

    @GetMapping("/stores/{maCH}")
    public ApiResponse<StoreResponse> getStoreDetail(@PathVariable Integer maCH) {
        return ApiResponse.success(storeModerationService.getStoreDetail(maCH));
    }

    @PostMapping("/stores/{maCH}/approve")
    public ApiResponse<StoreResponse> approveStore(@PathVariable Integer maCH) {
        return ApiResponse.success(storeModerationService.approveStore(maCH));
    }

    @PostMapping("/stores/{maCH}/reject")
    public ApiResponse<StoreResponse> rejectStore(
            @PathVariable Integer maCH, @Valid @RequestBody RejectRequest request) {
        return ApiResponse.success(storeModerationService.rejectStore(maCH, request));
    }
}
