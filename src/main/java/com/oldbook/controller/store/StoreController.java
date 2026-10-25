package com.oldbook.controller.store;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.store.CreateStoreRequest;
import com.oldbook.dto.store.StoreResponse;
import com.oldbook.dto.store.StoreRegistrationResponse;
import com.oldbook.dto.store.UpdateStoreRequest;
import com.oldbook.service.store.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService storeService;

    @GetMapping("/me")
    public ApiResponse<StoreResponse> getMyStore() {
        return ApiResponse.success(storeService.getMyStore());
    }

    @PostMapping
    public ApiResponse<StoreResponse> create(@Valid @RequestBody CreateStoreRequest request) {
        return ApiResponse.success(storeService.create(request));
    }

    @PostMapping("/register")
    public ApiResponse<StoreRegistrationResponse> register(@Valid @RequestBody CreateStoreRequest request) {
        return ApiResponse.success(storeService.registerAsCustomer(request));
    }

    @PutMapping("/me")
    public ApiResponse<StoreResponse> update(@Valid @RequestBody UpdateStoreRequest request) {
        return ApiResponse.success(storeService.updateMyStore(request));
    }
}
