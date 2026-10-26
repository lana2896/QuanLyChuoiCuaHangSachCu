package com.oldbook.service.notification;

import com.oldbook.service.order.CarrierChangedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Đẩy tin "đơn của hãng vừa thay đổi" qua kênh WebSocket /ws/shipping cho cổng của đúng hãng đang mở.
 * Tin không mang dữ liệu: cổng tự gọi lại REST /api/shipping/{maCode}/... để lấy danh sách mới.
 * Chỉ chạy sau khi giao dịch sửa đơn đã commit.
 */
@Component
@RequiredArgsConstructor
public class CarrierChangedPusher {

    public static final String LOAI_TIN = "VAN_CHUYEN_THAY_DOI";

    private static final Logger log = LoggerFactory.getLogger(CarrierChangedPusher.class);

    private final ShipmentSocketHandler socketHandler;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void push(CarrierChangedEvent event) {
        if (!socketHandler.isConnected(event.maCode())) {
            return;
        }

        try {
            socketHandler.gui(event.maCode(), objectMapper.writeValueAsString(Map.of("type", LOAI_TIN)));
        } catch (RuntimeException ex) {
            // Đẩy tức thì chỉ là tiện ích thêm; lỗi ở đây không được làm hỏng thao tác chính.
            log.warn("Không đẩy được cập nhật cho hãng {}: {}", event.maCode(), ex.getMessage());
        }
    }
}
