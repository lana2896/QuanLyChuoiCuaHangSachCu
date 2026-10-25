package com.oldbook.service.notification;

import com.oldbook.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Đẩy thông báo mới qua WebSocket cho người nhận đang mở trang.
 * Chỉ chạy sau khi giao dịch lưu thông báo đã commit, nên không bao giờ báo về một thông báo bị rollback.
 */
@Component
@RequiredArgsConstructor
public class NotificationPusher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPusher.class);

    private final NotificationSocketHandler socketHandler;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void day(NotificationSentEvent event) {
        for (NotificationSentEvent.BanGhi banGhi : event.thongBaos()) {
            Integer maTK = banGhi.maTKNhan();

            // Người nhận không mở trang nào thì khỏi truy vấn: họ sẽ thấy thông báo ở lần tải trang sau.
            if (!socketHandler.isConnected(maTK)) {
                continue;
            }

            try {
                long chuaDoc = notificationRepository.countByTaiKhoanNhan_MaTKAndDaDocFalse(maTK);
                socketHandler.gui(maTK, objectMapper.writeValueAsString(Map.of(
                        "type", "THONG_BAO_MOI",
                        "thongBao", banGhi.thongBao(),
                        "chuaDoc", chuaDoc
                )));
            } catch (RuntimeException ex) {
                // Đẩy tức thì chỉ là tiện ích thêm; lỗi ở đây không được làm hỏng thao tác chính.
                log.warn("Không đẩy được thông báo #{}: {}", banGhi.thongBao().maThongBao(), ex.getMessage());
            }
        }
    }
}
