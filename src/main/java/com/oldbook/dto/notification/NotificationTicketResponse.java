package com.oldbook.dto.notification;

// Vé dùng một lần để mở kết nối WebSocket /ws/notifications.
public record NotificationTicketResponse(String ticket) {
}
