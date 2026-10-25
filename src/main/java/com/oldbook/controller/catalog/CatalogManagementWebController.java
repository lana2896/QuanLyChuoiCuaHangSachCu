package com.oldbook.controller.catalog;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CatalogManagementWebController {

    @GetMapping("/management/catalog")
    public String catalogPage() {
        return "management/catalog";
    }
}
