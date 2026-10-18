package com.oldbook.service.shipping;

import com.oldbook.config.shipping.ShippingProperties;
import com.oldbook.entity.shipping.Shipment;
import com.oldbook.exception.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Gọi API REST của đơn vị vận chuyển ({@code shipping.sync.provider=rest}).
 * Địa chỉ, token, đường dẫn và vị trí các trường trong JSON được cấu hình cho từng đơn vị
 * trong {@code shipping.carriers.<MA>.*}; xem {@link ShippingProperties.Carrier}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "shipping.sync.provider", havingValue = "rest")
public class RestShippingCarrierClient implements ShippingCarrierClient {

    private final ShippingProperties properties;

    @Override
    public CarrierShipment createShipment(CarrierShipmentRequest request) {
        ShippingProperties.Carrier cfg = requireConfigured(request.maCode());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_order_code", String.valueOf(request.maDHCH()));
        body.put("to_name", request.tenNguoiNhan());
        body.put("to_phone", request.soDienThoaiNhan());
        body.put("to_address", request.diaChiGiao());
        body.put("cod_amount", request.tienThuHo());
        body.put("note", request.ghiChu());

        try {
            JsonNode response = client(cfg).post()
                    .uri(cfg.getCreatePath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String code = text(response, cfg.getTrackingCodePointer());
            if (code == null || code.isBlank()) {
                throw new BusinessException("Đơn vị vận chuyển không trả về mã vận đơn");
            }
            return new CarrierShipment(code.trim());
        } catch (RestClientException ex) {
            log.warn("Tạo vận đơn thất bại tại {}: {}", request.maCode(), ex.getMessage());
            throw new BusinessException(
                    "Không thể chuyển đơn sang đơn vị vận chuyển lúc này. Vui lòng thử lại sau");
        }
    }

    @Override
    public Optional<CarrierTracking> track(Shipment shipment) {
        String maCode = shipment.getDonViVanChuyen().getMaCode();
        ShippingProperties.Carrier cfg = requireConfigured(maCode);

        JsonNode response = client(cfg).get()
                .uri(cfg.getTrackPath(), Map.of("maVanDon", shipment.getMaVanDon()))
                .retrieve()
                .body(JsonNode.class);

        String status = text(response, cfg.getStatusPointer());
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CarrierTracking(status, text(response, cfg.getNotePointer()), null));
    }

    private ShippingProperties.Carrier requireConfigured(String maCode) {
        ShippingProperties.Carrier cfg = properties.carrier(maCode);
        if (cfg == null || cfg.getBaseUrl() == null || cfg.getBaseUrl().isBlank()) {
            throw new BusinessException("Đơn vị vận chuyển " + maCode + " chưa được cấu hình kết nối API");
        }
        return cfg;
    }

    private RestClient client(ShippingProperties.Carrier cfg) {
        RestClient.Builder builder = RestClient.builder().baseUrl(cfg.getBaseUrl());
        if (cfg.getApiToken() != null && !cfg.getApiToken().isBlank()) {
            builder.defaultHeader(cfg.getTokenHeader(), cfg.getApiToken());
        }
        return builder.build();
    }

    private static String text(JsonNode root, String pointer) {
        if (root == null || pointer == null || pointer.isBlank()) {
            return null;
        }
        JsonNode node = root.at(pointer);
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
