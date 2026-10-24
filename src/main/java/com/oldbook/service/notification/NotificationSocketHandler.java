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
 * Giữ các kết nối WebSocket đang mở theo mã tài khoản và đẩy tin xuống chúng.
 *
 * Kênh này chỉ đi một chiều từ máy chủ xuống. Trình duyệt chỉ gửi tin giữ kết nối, máy chủ bỏ qua nội dung.
 * Bảng kết nối nằm trong bộ nhớ của tiến trình này, đủ dùng khi ứng dụng chạy một instance.
 */
@Component
public class NotificationSocketHandler extends TextWebSocketHandler {

    // Tên thuộc tính do bước bắt tay gắn vào phiên sau khi kiểm tra vé.
    public static final String THUOC_TINH_MA_TK = "maTK";

    // Mỗi tab trình duyệt là một kết nối; quá số này thì từ chối để một tài khoản không chiếm hết lượt kết nối.
    private static final int TOI_DA_MOI_TAI_KHOAN = 5;
    private static final int THOI_GIAN_GUI_TOI_DA_MS = 5_000;
    private static final int BO_DEM_TOI_DA_BYTES = 64 * 1024;

    private static final Logger log = LoggerFactory.getLogger(NotificationSocketHandler.class);

    private final Map<Integer, Map<String, WebSocketSession>> phienTheoTaiKhoan = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer maTK = maTK(session);
        if (maTK == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Map<String, WebSocketSession> phien =
                phienTheoTaiKhoan.computeIfAbsent(maTK, key -> new ConcurrentHashMap<>());

        if (phien.size() >= TOI_DA_MOI_TAI_KHOAN) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Quá nhiều kết nối"));
            return;
        }

        // Nhiều luồng có thể cùng đẩy tin cho một phiên; lớp bọc này xếp hàng các lần gửi.
        phien.put(session.getId(),
                new ConcurrentWebSocketSessionDecorator(session, THOI_GIAN_GUI_TOI_DA_MS, BO_DEM_TOI_DA_BYTES));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer maTK = maTK(session);
        if (maTK == null) {
            return;
        }
        phienTheoTaiKhoan.computeIfPresent(maTK, (key, phien) -> {
            phien.remove(session.getId());
            return phien.isEmpty() ? null : phien;
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Tin giữ kết nối từ trình duyệt: không cần trả lời.
    }

    public boolean isConnected(Integer maTK) {
        return phienTheoTaiKhoan.containsKey(maTK);
    }

    public void gui(Integer maTK, String noiDung) {
        Map<String, WebSocketSession> phien = phienTheoTaiKhoan.get(maTK);
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
                // Kết nối hỏng thì bỏ: trình duyệt vẫn thấy thông báo khi tải lại trang.
                log.debug("Không đẩy được thông báo cho tài khoản {}: {}", maTK, ex.getMessage());
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (Exception ignored) {
                    // đã đóng sẵn
                }
            }
        }
    }

    private static Integer maTK(WebSocketSession session) {
        Object value = session.getAttributes().get(THUOC_TINH_MA_TK);
        return value instanceof Integer maTK ? maTK : null;
    }
}
