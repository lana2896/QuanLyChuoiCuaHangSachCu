package com.oldbook.controller.catalog;

import com.oldbook.constant.auth.Role;
import com.oldbook.dto.catalog.CreateReviewRequest;
import com.oldbook.dto.catalog.ReviewResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.catalog.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping(value = "/order-items/{maCTDH}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Integer maCTDH,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.createReview(getCustomerId(), maCTDH, request)));
    }

    @PostMapping(value = "/order-items/{maCTDH}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReviewResponse>> createReviewWithMedia(
            @PathVariable Integer maCTDH,
            @Valid @ModelAttribute CreateReviewRequest request,
            @RequestParam(name = "files", required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.createReview(getCustomerId(), maCTDH, request, files)));
    }

    private Integer getCustomerId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getDetails() instanceof AuthenticatedUserDetails user)
                || user.maND() == null || !Role.KHACH_HANG.name().equals(user.vaiTro())) {
            throw new AccessDeniedException("Chỉ khách hàng mới được đánh giá sách đã mua");
        }
        return user.maND();
    }
}
