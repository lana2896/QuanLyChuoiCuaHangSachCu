package com.oldbook.controller.cart;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartWebController {

    @GetMapping("/cart")
    public String cartPage() {
        return "cart";
    }
}
