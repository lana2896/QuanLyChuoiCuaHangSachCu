package com.oldbook.controller.store;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StoreWebController {

    @GetMapping({"/store", "/store/books"})
    public String storePage() {
        return "store-books";
    }

    @GetMapping("/store/register")
    public String registerPage() {
        return "store-register";
    }

    @GetMapping("/store-moderation")
    public String moderationPage() {
        return "store-moderation";
    }
}
