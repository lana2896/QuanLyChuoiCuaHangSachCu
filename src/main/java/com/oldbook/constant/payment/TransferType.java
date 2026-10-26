package com.oldbook.constant.payment;

/** Hai loại tiền sàn phải chuyển đi. Tiền tố dùng cho nội dung chuyển khoản, ví dụ HT15, CT8. */
public enum TransferType {
    HOAN_TIEN_KHACH("HT"),
    CHI_CUA_HANG("CT");

    private final String tienTo;

    TransferType(String tienTo) {
        this.tienTo = tienTo;
    }

    public String getTienTo() {
        return tienTo;
    }
}
