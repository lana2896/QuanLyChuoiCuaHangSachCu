package com.oldbook.controller.payment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VoucherWebController {

    @GetMapping("/management/vouchers")
    public String voucherManagementPage() {
        return "management/vouchers";
    }

    @GetMapping("/store/vouchers")
    public String storeVoucherPage() {
        return "store-vouchers";
    }
}
