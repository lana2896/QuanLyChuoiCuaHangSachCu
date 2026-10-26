package com.oldbook.controller.payment;

import com.oldbook.service.payment.CassoSignatureVerifier;
import com.oldbook.service.payment.PaymentConfirmationService;
import com.oldbook.service.payment.PaymentConfirmationService.IncomingTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Nơi Casso gọi vào mỗi khi tài khoản của sàn có giao dịch (Webhook V2).
 * Không đăng nhập: yêu cầu chỉ được nhận khi chữ ký X-Casso-Signature đúng.
 *
 * Casso coi mọi phản hồi khác 200 là thất bại và sẽ gửi lại, nên giao dịch không khớp phiên nào
 * vẫn được trả lời thành công; chỉ lỗi thật của hệ thống mới để Casso gửi lại.
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/casso")
@RequiredArgsConstructor
public class CassoWebhookController {

    private static final DateTimeFormatter DINH_DANG_THOI_GIAN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CassoSignatureVerifier signatureVerifier;
    private final PaymentConfirmationService paymentConfirmationService;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> nhan(
            @RequestHeader(name = "X-Casso-Signature", required = false) String chuKy,
            @RequestBody String noiDung
    ) {
        if (!signatureVerifier.isValid(chuKy, noiDung)) {
            log.warn("Từ chối webhook Casso: sai hoặc thiếu chữ ký");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        JsonNode goc = objectMapper.readTree(noiDung);
        JsonNode duLieu = goc.path("data");

        if (goc.path("error").asInt(0) == 0 && !duLieu.isMissingNode() && !duLieu.isNull()) {
            // Webhook V2 gửi một giao dịch mỗi lần; bản cũ gửi một mảng. Nhận cả hai.
            if (duLieu.isArray()) {
                duLieu.forEach(giaoDich -> paymentConfirmationService.process(docGiaoDich(giaoDich, noiDung)));
            } else {
                paymentConfirmationService.process(docGiaoDich(duLieu, noiDung));
            }
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    private static IncomingTransaction docGiaoDich(JsonNode giaoDich, String noiDungGoc) {
        return new IncomingTransaction(
                maGiaoDich(giaoDich, noiDungGoc),
                chu(giaoDich, "reference"),
                giaoDich.path("amount").isNumber() ? giaoDich.path("amount").decimalValue() : BigDecimal.ZERO,
                chu(giaoDich, "description"),
                thoiGian(chu(giaoDich, "transactionDateTime")),
                chu(giaoDich, "accountNumber"),
                chu(giaoDich, "counterAccountName"),
                chu(giaoDich, "counterAccountNumber"),
                chu(giaoDich, "counterAccountBankId")
        );
    }

    // Giao dịch thật có id riêng. Giao dịch do nút "Gọi thử" gửi có id bằng 0, nên dùng mã tham chiếu thay thế.
    private static String maGiaoDich(JsonNode giaoDich, String noiDungGoc) {
        JsonNode id = giaoDich.path("id");
        if (id.isNumber() && id.asLong() > 0) {
            return String.valueOf(id.asLong());
        }
        String thamChieu = chu(giaoDich, "reference");
        if (thamChieu != null) {
            return "REF-" + thamChieu;
        }
        try {
            byte[] bam = MessageDigest.getInstance("SHA-256").digest(noiDungGoc.getBytes(StandardCharsets.UTF_8));
            return "HASH-" + HexFormat.of().formatHex(bam, 0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String chu(JsonNode node, String truong) {
        JsonNode giaTri = node.path(truong);
        if (giaTri.isMissingNode() || giaTri.isNull()) {
            return null;
        }
        String chu = giaTri.asText().trim();
        return chu.isEmpty() ? null : chu;
    }

    private static LocalDateTime thoiGian(String giaTri) {
        if (giaTri == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(giaTri, DINH_DANG_THOI_GIAN);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
