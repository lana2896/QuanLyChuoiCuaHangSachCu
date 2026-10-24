package com.oldbook.config.notification;

import com.oldbook.service.notification.NotificationSocketHandler;
import com.oldbook.service.notification.NotificationTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * Đăng ký kênh WebSocket /ws/notifications.
 * Việc xác thực nằm ở bước bắt tay: phải có vé hợp lệ do POST /api/thong-bao/ticket cấp.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class NotificationSocketConfig implements WebSocketConfigurer {

    private final NotificationSocketHandler handler;
    private final NotificationTicketService ticketService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notifications")
                // Tự so nguồn gốc ở dưới (theo tên máy chủ), vì sau proxy của nhà cung cấp hosting
                // giao thức http/https của request không còn khớp với trình duyệt.
                .setAllowedOriginPatterns("*")
                .addInterceptors(new TicketHandshakeInterceptor());
    }

    private final class TicketHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes
        ) {
            if (!isSameHost(request)) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            String ticket = UriComponentsBuilder.fromUri(request.getURI())
                    .build()
                    .getQueryParams()
                    .getFirst("ticket");

            Optional<Integer> maTK = ticketService.consume(ticket);
            if (maTK.isEmpty()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            attributes.put(NotificationSocketHandler.THUOC_TINH_MA_TK, maTK.get());
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception
        ) {
            // không cần làm gì
        }

        // Trang mở socket phải là trang của chính ứng dụng. Client không phải trình duyệt thì không gửi Origin.
        private boolean isSameHost(ServerHttpRequest request) {
            String origin = request.getHeaders().getOrigin();
            if (origin == null) {
                return true;
            }

            String host = request.getHeaders().getFirst("X-Forwarded-Host");
            if (host == null || host.isBlank()) {
                host = request.getHeaders().getFirst(HttpHeaders.HOST);
            }

            try {
                URI uri = URI.create(origin);
                String originHost = uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
                return originHost != null && originHost.equalsIgnoreCase(host);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    }
}
