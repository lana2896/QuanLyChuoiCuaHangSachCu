package com.oldbook.controller.payment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Các trang thanh toán. HTML công khai; dữ liệu lấy qua API có phân quyền, và JS kiểm vai trò để hiện thông báo. */
@Controller
public class PaymentWebController {

    /** Trang quét mã: /payment/pay?maPhien=... */
    @GetMapping("/payment/pay")
    public String payPage() {
        return "payment/pay";
    }

    @GetMapping("/payment/refunds")
    public String refundsPage() {
        return "payment/refunds";
    }

    @GetMapping("/management/payments")
    public String managementPage() {
        return "payment/management";
    }
}
