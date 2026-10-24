package com.oldbook.service.notification;

import com.oldbook.dto.notification.NotificationResponse;

import java.util.List;

/**
 * Phát ra sau khi lưu thông báo, để kênh đẩy báo cho những người nhận đang mở trang.
 * Chỉ chứa các thông báo chưa đọc: bản lưu sẵn "đã đọc" của chính người thực hiện thì không cần báo.
 */
public record NotificationSentEvent(List<BanGhi> thongBaos) {

    public record BanGhi(Integer maTKNhan, NotificationResponse thongBao) {
    }
}
