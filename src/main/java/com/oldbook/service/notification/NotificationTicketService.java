package com.oldbook.service.notification;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vé dùng một lần để mở kết nối WebSocket nhận thông báo.
 *
 * Trình duyệt không gắn được header Authorization vào lúc bắt tay WebSocket, còn đặt JWT lên URL thì lộ
 * trong nhật ký máy chủ. Vì vậy trình duyệt xin vé qua REST (đã qua bộ lọc JWT như mọi API khác),
 * rồi mở socket kèm vé. Vé chỉ sống vài chục giây và dùng xong là hủy.
 */
@Service
public class NotificationTicketService {

    static final Duration THOI_HAN = Duration.ofSeconds(30);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ve> veDangCho = new ConcurrentHashMap<>();
    private final Clock clock;

    public NotificationTicketService() {
        this(Clock.systemUTC());
    }

    NotificationTicketService(Clock clock) {
        this.clock = clock;
    }

    public String issue(Integer maTK) {
        Instant now = clock.instant();

        // Vé không được dùng thì tự hết hạn; dọn luôn ở đây để bảng không phình ra.
        veDangCho.values().removeIf(ve -> !ve.hetHan().isAfter(now));

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String ma = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        veDangCho.put(ma, new Ve(maTK, now.plus(THOI_HAN)));
        return ma;
    }

    /** Trả về mã tài khoản của vé nếu vé còn hạn. Dù kết quả thế nào, vé cũng không dùng lại được. */
    public Optional<Integer> consume(String ma) {
        if (ma == null || ma.isBlank()) {
            return Optional.empty();
        }

        Ve ve = veDangCho.remove(ma);
        if (ve == null || !ve.hetHan().isAfter(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(ve.maTK());
    }

    private record Ve(Integer maTK, Instant hetHan) {
    }
}
