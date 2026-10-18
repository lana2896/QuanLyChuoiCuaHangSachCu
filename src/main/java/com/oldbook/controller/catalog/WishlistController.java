package com.oldbook.controller.catalog;

import com.oldbook.constant.auth.Role;
import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.catalog.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookResponse>>> getWishlist() {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getWishlist(getCustomerUserId())));
    }

    @PutMapping("/{maSach}")
    public ResponseEntity<ApiResponse<BookResponse>> addBook(@PathVariable Integer maSach) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.addBook(getCustomerUserId(), maSach)));
    }

    @DeleteMapping("/{maSach}")
    public ResponseEntity<ApiResponse<Void>> removeBook(@PathVariable Integer maSach) {
        wishlistService.removeBook(getCustomerUserId(), maSach);
        return ResponseEntity.ok(ApiResponse.success("Đã bỏ yêu thích sách"));
    }

    private Integer getCustomerUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getDetails() instanceof AuthenticatedUserDetails user)
                || user.maND() == null || !Role.KHACH_HANG.name().equals(user.vaiTro())) {
            throw new AccessDeniedException("Chỉ khách hàng mới sử dụng được danh sách yêu thích");
        }
        return user.maND();
    }
}
