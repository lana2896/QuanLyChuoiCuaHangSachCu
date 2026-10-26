package com.oldbook.service.notification;

import com.oldbook.service.order.OrderChangedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Đẩy tin "đơn hàng vừa thay đổi" qua cùng kênh WebSocket /ws/notifications cho khách và chủ chi nhánh đang mở trang.
 * Tin chỉ báo mã đơn, không mang dữ liệu: trang nhận được sẽ tự gọi lại REST để lấy danh sách đúng quyền.
 * Chỉ chạy sau khi giao dịch sửa đơn đã commit.
 */
@Component
@RequiredArgsConstructor
public class OrderChangedPusher {

    public static final String LOAI_TIN = "DON_HANG_THAY_DOI";

    private static final Logger log = LoggerFactory.getLogger(OrderChangedPusher.class);

    private final NotificationSocketHandler socketHandler;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void push(OrderChangedEvent event) {
        String noiDung = null;

        for (Integer maTK : event.maTKNhan()) {
            // Không ai mở trang thì khỏi soạn tin: họ sẽ thấy đơn mới ở lần tải trang sau.
            if (!socketHandler.isConnected(maTK)) {
                continue;
            }

            try {
                if (noiDung == null) {
                    Map<String, Object> tin = new LinkedHashMap<>();
                    tin.put("type", LOAI_TIN);
                    tin.put("maDH", event.maDH());
                    tin.put("maDHCH", event.maDHCH());
                    noiDung = objectMapper.writeValueAsString(tin);
                }
                socketHandler.gui(maTK, noiDung);
            } catch (RuntimeException ex) {
                // Đẩy tức thì chỉ là tiện ích thêm; lỗi ở đây không được làm hỏng thao tác chính.
                log.warn("Không đẩy được cập nhật đơn #{}: {}", event.maDHCH(), ex.getMessage());
            }
        }
    }
}
