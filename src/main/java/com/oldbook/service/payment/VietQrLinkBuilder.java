package com.oldbook.service.payment;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Dựng địa chỉ ảnh mã VietQR (dịch vụ Quick Link của vietqr.io, không cần khóa API).
 * Người quét mã thấy sẵn tài khoản nhận, số tiền và nội dung chuyển khoản.
 */
@Component
public class VietQrLinkBuilder {

    private static final String BASE_URL = "https://img.vietqr.io/image/";

    /** Trả về null khi thiếu tài khoản nhận, để giao diện biết là chưa hiện được mã. */
    public String build(String maNganHang, String soTaiKhoan, String tenChuTaiKhoan, BigDecimal soTien, String noiDung) {
        if (trong(maNganHang) || trong(soTaiKhoan) || soTien == null) {
            return null;
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(BASE_URL + maNganHang.trim() + "-" + soTaiKhoan.trim() + "-compact2.png")
                // VND không có phần lẻ.
                .queryParam("amount", soTien.setScale(0, RoundingMode.HALF_UP).toPlainString())
                .queryParam("addInfo", noiDung == null ? "" : noiDung);

        if (!trong(tenChuTaiKhoan)) {
            builder.queryParam("accountName", tenChuTaiKhoan.trim());
        }

        return builder.encode().build().toUriString();
    }

    private static boolean trong(String value) {
        return value == null || value.isBlank();
    }
}
