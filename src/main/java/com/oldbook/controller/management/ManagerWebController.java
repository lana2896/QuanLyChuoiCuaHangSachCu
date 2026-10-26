package com.oldbook.controller.management;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang của cổng Quản lý. HTML công khai (JWT nằm ở localStorage nên request này không mang danh tính);
 * dữ liệu lấy qua /api/management/** có phân quyền, và JS kiểm vai trò để hiện thông báo.
 * Kiểm duyệt, Đơn vị vận chuyển và Thanh toán giữ đường dẫn cũ ở controller của từng phần.
 */
@Controller
public class ManagerWebController {

    @GetMapping({"/management", "/management/overview"})
    public String overview() {
        return "management/overview";
    }

    @GetMapping("/management/stats")
    public String stats() {
        return "management/stats";
    }

    @GetMapping("/management/commissions")
    public String commissions() {
        return "management/commissions";
    }

    /** Hồ sơ cá nhân của người đang dùng cổng Quản lý; đọc và lưu qua /api/profile. */
    @GetMapping("/management/profile")
    public String profile() {
        return "management/profile";
    }
}
