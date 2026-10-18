package com.oldbook.controller.catalog;

import com.oldbook.constant.auth.Role;
import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.catalog.RecentlyViewedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recently-viewed")
@RequiredArgsConstructor
public class RecentlyViewedController {

    private final RecentlyViewedService recentlyViewedService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookResponse>>> getRecentlyViewed() {
        return ResponseEntity.ok(ApiResponse.success(recentlyViewedService.getRecentlyViewed(getCustomerUserId())));
    }

    @PutMapping("/{maSach}")
    public ResponseEntity<ApiResponse<BookResponse>> recordView(@PathVariable Integer maSach) {
        return ResponseEntity.ok(ApiResponse.success(recentlyViewedService.recordView(getCustomerUserId(), maSach)));
    }

    private Integer getCustomerUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getDetails() instanceof AuthenticatedUserDetails user)
                || user.maND() == null || !Role.KHACH_HANG.name().equals(user.vaiTro())) {
            throw new AccessDeniedException("Chỉ khách hàng mới sử dụng được lịch sử sách đã xem");
        }
        return user.maND();
    }
}
