package com.oldbook.controller.identity;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.identity.AddressRequest;
import com.oldbook.dto.identity.AddressResponse;
import com.oldbook.service.identity.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(addressService.getAll(getMaND())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> create(
            @Valid @RequestBody AddressRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(addressService.create(getMaND(), request))
        );
    }

    @PutMapping("/{maDiaChi}")
    public ResponseEntity<ApiResponse<AddressResponse>> update(
            @PathVariable Integer maDiaChi,
            @Valid @RequestBody AddressRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        addressService.update(getMaND(), maDiaChi, request)
                )
        );
    }

    @PatchMapping("/{maDiaChi}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(
            @PathVariable Integer maDiaChi
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        addressService.setDefault(getMaND(), maDiaChi)
                )
        );
    }

    @DeleteMapping("/{maDiaChi}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer maDiaChi
    ) {
        addressService.delete(getMaND(), maDiaChi);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa địa chỉ"));
    }

    private Integer getMaND() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return (Integer) authentication.getPrincipal();
    }
}
