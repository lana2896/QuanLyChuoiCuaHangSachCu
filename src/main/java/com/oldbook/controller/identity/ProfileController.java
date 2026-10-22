package com.oldbook.controller.identity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.identity.ProfileResponse;
import com.oldbook.dto.identity.AvatarUploadResponse;
import com.oldbook.dto.identity.UpdateProfileRequest;
import com.oldbook.service.identity.ProfileService;
import com.oldbook.service.media.ImageUploadService;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ImageUploadService imageUploadService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AvatarUploadResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        profileService.getProfile(getMaND());
        return ApiResponse.success(new AvatarUploadResponse(
                imageUploadService.upload(file, "oldbook/avatars")));
    }

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