package com.oldbook.constant.payment;

/** Kết quả đối chiếu một giao dịch ngân hàng với các phiên thanh toán. */
public enum ReconciliationResult {
    /** Vừa nhận, chưa đối chiếu xong. */
    DANG_XU_LY,
    /** Khớp một phiên và đã tạo được đơn hàng. */
    KHOP,
    /** Không tìm thấy mã thanh toán nào trong nội dung, hoặc không phải tiền vào. */
    KHONG_KHOP,
    /** Khớp một phiên nhưng không tạo được đơn; đã lập yêu cầu hoàn tiền. */
    CAN_HOAN_TIEN
}
