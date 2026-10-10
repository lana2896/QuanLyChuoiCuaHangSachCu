package com.oldbook.controller.identity;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountWebController {

    @GetMapping("/admin/accounts")
    public String accountsPage() {
        return "admin-accounts";
    }
}
