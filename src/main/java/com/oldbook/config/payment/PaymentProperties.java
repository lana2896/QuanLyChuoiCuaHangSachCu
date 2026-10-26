package com.oldbook.config.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình thanh toán chuyển khoản. Giá trị lấy từ biến môi trường, không đặt trong application.properties:
 * PAYMENT_BANK_BIN, PAYMENT_BANK_ACCOUNT_NO, PAYMENT_BANK_ACCOUNT_NAME, PAYMENT_CASSO_CHECKSUM_KEY, PAYMENT_SESSION_MINUTES.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private Bank bank = new Bank();

    private Casso casso = new Casso();

    /** Thời gian giữ hàng và thời gian sống của mã QR, tính bằng phút. */
    private int sessionMinutes = 5;

    /** Chưa khai đủ tài khoản của sàn thì không cho chọn chuyển khoản. */
    public boolean isEnabled() {
        return coGiaTri(bank.getBin()) && coGiaTri(bank.getAccountNo()) && coGiaTri(bank.getAccountName());
    }

    private static boolean coGiaTri(String value) {
        return value != null && !value.isBlank();
    }

    /** Tài khoản ngân hàng của sàn, nơi khách chuyển tiền vào. */
    @Getter
    @Setter
    public static class Bank {
        /** Mã BIN 6 số của ngân hàng theo Napas, ví dụ 970422. */
        private String bin;
        private String accountNo;
        private String accountName;
    }

    @Getter
    @Setter
    public static class Casso {
        /** "Key bảo mật" của tích hợp Webhook V2, dùng để kiểm chữ ký X-Casso-Signature. */
        private String checksumKey;
    }
}
