package com.oldbook.controller.notification;

import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.notification.UnreadNotificationResponse;
import com.oldbook.dto.notification.NotificationResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thông báo của chính tài khoản đang đăng nhập, dùng được cho mọi vai trò.
 * Mọi thao tác đều lấy mã tài khoản từ JWT, không nhận từ tham số, nên không xem được thông báo của người khác.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> danhSach(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return ApiResponse.success(
                notificationService.danhSach(currentMaTK(), page, size)
        );
    }

    @GetMapping("/unread")
    public ApiResponse<UnreadNotificationResponse> countUnread() {

        return ApiResponse.success(
                new UnreadNotificationResponse(
                        notificationService.countUnread(currentMaTK())
                )
        );
    }

    @PutMapping("/{maThongBao}/read")
    public ApiResponse<Void> markRead(
            @PathVariable Integer maThongBao
    ) {

        notificationService.markRead(currentMaTK(), maThongBao);

        return ApiResponse.success("Đã đánh dấu đã đọc");
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {

        notificationService.markAllAsRead(currentMaTK());

        return ApiResponse.success("Đã đánh dấu tất cả đã đọc");
    }

    private Integer currentMaTK() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return ((AuthenticatedUserDetails) authentication.getDetails()).maTK();
    }
}
