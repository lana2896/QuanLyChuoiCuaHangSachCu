package com.oldbook.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giữ các kết nối WebSocket của cổng đơn vị vận chuyển theo mã hãng và đẩy tin xuống chúng.
 *
 * Cổng này không đăng nhập nên không dùng được kênh theo tài khoản của NotificationSocketHandler:
 * người mở kết nối được xác định bằng mã hãng trên URL, đã kiểm tra ở bước bắt tay.
 * Kênh chỉ đi một chiều từ máy chủ xuống; bảng kết nối nằm trong bộ nhớ, đủ dùng khi ứng dụng chạy một instance.
 */
@Component
public class ShipmentSocketHandler extends TextWebSocketHandler {

    // Tên thuộc tính do bước bắt tay gắn vào phiên sau khi kiểm tra mã hãng.
    public static final String THUOC_TINH_MA_CODE = "maCode";

    private static final int TOI_DA_MOI_HANG = 20;
    private static final int THOI_GIAN_GUI_TOI_DA_MS = 5_000;
    private static final int BO_DEM_TOI_DA_BYTES = 64 * 1024;

    private static final Logger log = LoggerFactory.getLogger(ShipmentSocketHandler.class);

    private final Map<String, Map<String, WebSocketSession>> phienTheoHang = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String maCode = maCode(session);
        if (maCode == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Map<String, WebSocketSession> phien =
                phienTheoHang.computeIfAbsent(maCode, key -> new ConcurrentHashMap<>());

        if (phien.size() >= TOI_DA_MOI_HANG) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Quá nhiều kết nối"));
            return;
        }

        phien.put(session.getId(),
                new ConcurrentWebSocketSessionDecorator(session, THOI_GIAN_GUI_TOI_DA_MS, BO_DEM_TOI_DA_BYTES));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String maCode = maCode(session);
        if (maCode == null) {
            return;
        }
        phienTheoHang.computeIfPresent(maCode, (key, phien) -> {
            phien.remove(session.getId());
            return phien.isEmpty() ? null : phien;
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Tin giữ kết nối từ trình duyệt: không cần trả lời.
    }

    public boolean isConnected(String maCode) {
        return phienTheoHang.containsKey(maCode);
    }

    public void gui(String maCode, String noiDung) {
        Map<String, WebSocketSession> phien = phienTheoHang.get(maCode);
        if (phien == null) {
            return;
        }

        TextMessage message = new TextMessage(noiDung);
        for (WebSocketSession session : phien.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (Exception ex) {
                // Kết nối hỏng thì bỏ: trang vẫn đúng sau khi nối lại hoặc tải lại.
                log.debug("Không đẩy được tin cho hãng {}: {}", maCode, ex.getMessage());
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (Exception ignored) {
                    // đã đóng sẵn
                }
            }
        }
    }

    private static String maCode(WebSocketSession session) {
        Object value = session.getAttributes().get(THUOC_TINH_MA_CODE);
        return value instanceof String maCode ? maCode : null;
    }
}
