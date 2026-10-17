package com.oldbook.controller.identity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.identity.ProfileResponse;
import com.oldbook.dto.identity.UpdateProfileRequest;
import com.oldbook.service.identity.ProfileService;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {

        Integer maND = getMaND();

        ProfileResponse response =
                profileService.getProfile(maND);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        Integer maND = getMaND();

        ProfileResponse response =
                profileService.updateProfile(
                        maND,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    private Integer getMaND() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return (Integer) authentication.getPrincipal();
    }
}