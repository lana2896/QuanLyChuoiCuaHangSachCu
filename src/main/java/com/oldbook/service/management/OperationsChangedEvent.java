package com.oldbook.service.management;

/**
 * Dữ liệu vận hành vừa đổi (khách khai tài khoản nhận hoàn, Quản lý bấm "Đã chuyển", đổi hoa hồng...).
 * OperationsPusher đẩy tin này cho mọi Quản lý đang mở trang sau khi giao dịch commit.
 *
 * @param phan phần màn hình cần tải lại: payouts, refunds, transactions, commissions, moderation
 */
public record OperationsChangedEvent(String phan) {

    public static final String PAYOUTS = "payouts";
    public static final String REFUNDS = "refunds";
    public static final String TRANSACTIONS = "transactions";
    public static final String COMMISSIONS = "commissions";
    public static final String MODERATION = "moderation";
}
