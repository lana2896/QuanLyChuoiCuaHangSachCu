package com.oldbook.controller.catalog;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WishlistWebController {

    @GetMapping("/wishlist")
    public String wishlistPage() {
        return "wishlist";
    }
}
