package com.oldbook.config.shipping;

import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.service.notification.ShipmentSocketHandler;
import com.oldbook.service.shipping.ShippingCarrierResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * Đăng ký kênh WebSocket /ws/shipping?maCode=... cho cổng đơn vị vận chuyển.
 * Cổng không đăng nhập (xem ShippingCarrierResolver), nên bước bắt tay chỉ kiểm tra mã hãng có tồn tại
 * và còn hoạt động, giống điều kiện để mở được trang /shipping/{maCode}.
 */
@Configuration
@RequiredArgsConstructor
public class ShipmentSocketConfig implements WebSocketConfigurer {

    private final ShipmentSocketHandler handler;
    private final ShippingCarrierResolver shippingCarrierResolver;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/shipping")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new MaCodeHandshakeInterceptor());
    }

    private final class MaCodeHandshakeInterceptor implements HandshakeInterceptor {

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

            String maCode = UriComponentsBuilder.fromUri(request.getURI())
                    .build()
                    .getQueryParams()
                    .getFirst("maCode");

            Optional<ShippingCarrier> donVi = shippingCarrierResolver.find(maCode);
            if (donVi.isEmpty()) {
                response.setStatusCode(HttpStatus.NOT_FOUND);
                return false;
            }
            if (!ShippingCarrierResolver.isActive(donVi.get())) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put(ShipmentSocketHandler.THUOC_TINH_MA_CODE, donVi.get().getMaCode());
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
