package com.oldbook.service.management;

import com.oldbook.constant.auth.AccountStatus;
import com.oldbook.constant.auth.Role;
import com.oldbook.entity.identity.Account;
import com.oldbook.repository.notification.NotificationRepository;
import com.oldbook.service.notification.NotificationSocketHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Đẩy tin VAN_HANH_THAY_DOI qua WebSocket /ws/notifications cho mọi Quản lý đang mở trang, để màn hình của họ tự cập nhật.
 * Tin chỉ đi qua socket, không lưu vào bảng thong_bao. Chỉ chạy sau khi giao dịch đã commit.
 */
@Component
@RequiredArgsConstructor
public class OperationsPusher {

    public static final String LOAI_TIN = "VAN_HANH_THAY_DOI";

    private static final Logger log = LoggerFactory.getLogger(OperationsPusher.class);

    private final NotificationSocketHandler socketHandler;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void push(OperationsChangedEvent event) {
        try {
            String tin = objectMapper.writeValueAsString(Map.of("type", LOAI_TIN, "phan", event.phan()));
            for (Account quanLy : notificationRepository.findAccountsByRoleAndStatus(
                    Role.QUAN_LY.name(), AccountStatus.HOAT_DONG.name())) {
                if (socketHandler.isConnected(quanLy.getMaTK())) {
                    socketHandler.gui(quanLy.getMaTK(), tin);
                }
            }
        } catch (RuntimeException ex) {
            // Đẩy tức thì chỉ là tiện ích thêm; lỗi ở đây không được làm hỏng thao tác chính.
            log.warn("Không đẩy được thay đổi vận hành {}: {}", event.phan(), ex.getMessage());
        }
    }
}
