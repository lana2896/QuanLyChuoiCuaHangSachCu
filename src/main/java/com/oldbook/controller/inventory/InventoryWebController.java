package com.oldbook.controller.inventory;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang HTML của phần kho chi nhánh; dữ liệu lấy qua API có phân quyền. */
@Controller
public class InventoryWebController {

    @GetMapping("/store/transfers")
    public String storeTransfers() {
        return "store-transfers";
    }

    @GetMapping("/management/inventory")
    public String chainInventory() {
        return "management/inventory";
    }
}
