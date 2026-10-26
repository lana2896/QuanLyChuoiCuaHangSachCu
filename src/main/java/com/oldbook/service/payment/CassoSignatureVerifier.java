package com.oldbook.service.payment;

import com.oldbook.config.payment.PaymentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kiểm chữ ký của Casso Webhook V2, làm theo mã mẫu Java của Casso
 * (github.com/CassoHQ/casso-webhook-v2-verify-signature):
 *
 *   header   X-Casso-Signature: t=<thời gian>,v1=<chữ ký dạng hex>
 *   chuỗi ký t + "." + JSON của nội dung, sau khi sắp các khóa theo thứ tự chữ cái (kể cả đối tượng lồng)
 *   chữ ký   HMAC-SHA512(chuỗi ký, khóa bảo mật của tích hợp)
 *
 * Webhook mở cho mọi nguồn, nên đây là thứ duy nhất chứng minh yêu cầu đến từ Casso.
 */
@Component
@RequiredArgsConstructor
public class CassoSignatureVerifier {

    private static final String THUAT_TOAN = "HmacSHA512";

    // Mapper riêng với cấu hình mặc định: chuỗi JSON dựng lại phải giống hệt chuỗi Casso đã ký.
    // Giữ số thực ở dạng BigDecimal để "1500.50" không bị viết lại thành "1500.5".
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private final PaymentProperties properties;

    public boolean isValid(String header, String noiDung) {
        String khoa = properties.getCasso().getChecksumKey();
        if (khoa == null || khoa.isBlank() || header == null || noiDung == null) {
            return false;
        }

        String thoiGian = null;
        String chuKy = null;
        for (String phan : header.split(",")) {
            String p = phan.trim();
            if (p.startsWith("t=")) {
                thoiGian = p.substring(2);
            } else if (p.startsWith("v1=")) {
                chuKy = p.substring(3);
            }
        }
        if (thoiGian == null || thoiGian.isBlank() || chuKy == null || chuKy.isBlank()) {
            return false;
        }

        try {
            byte[] mongDoi = computeSignature(khoa, thoiGian, noiDung).getBytes(StandardCharsets.UTF_8);
            byte[] nhanDuoc = chuKy.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
            // So sánh thời gian cố định, không để lộ chữ ký đúng qua thời gian phản hồi.
            return MessageDigest.isEqual(mongDoi, nhanDuoc);
        } catch (RuntimeException | GeneralSecurityException ex) {
            return false;
        }
    }

    /** Chữ ký hex chữ thường của một nội dung. Công khai để script giả lập và test tạo được yêu cầu hợp lệ. */
    public static String computeSignature(String khoa, String thoiGian, String noiDung) throws GeneralSecurityException {
        Object duLieu = sapXep(MAPPER.readValue(noiDung, Object.class));
        String chuoiKy = thoiGian + "." + MAPPER.writeValueAsString(duLieu);

        Mac mac = Mac.getInstance(THUAT_TOAN);
        mac.init(new SecretKeySpec(khoa.getBytes(StandardCharsets.UTF_8), THUAT_TOAN));
        return HexFormat.of().formatHex(mac.doFinal(chuoiKy.getBytes(StandardCharsets.UTF_8)));
    }

    // Giống mã mẫu của Casso: chỉ sắp khóa của đối tượng, không đụng tới thứ tự phần tử trong mảng.
    private static Object sapXep(Object giaTri) {
        if (!(giaTri instanceof Map<?, ?> map)) {
            return giaTri;
        }
        TreeMap<String, Object> daSapXep = new TreeMap<>();
        map.forEach((khoa, con) -> daSapXep.put(String.valueOf(khoa), sapXep(con)));
        return daSapXep;
    }
}
