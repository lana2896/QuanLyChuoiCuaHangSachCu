package com.oldbook.controller.order;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderWebController {

    @GetMapping("/checkout")
    public String checkoutPage() {
        return "checkout";
    }

    @GetMapping("/orders")
    public String ordersPage() {
        return "orders";
    }

    @GetMapping("/store/orders")
    public String storeOrdersPage() {
        return "store-orders";
    }

    @GetMapping("/order-success")
    public String orderSuccessPage() {
        return "order-success";
    }
}
