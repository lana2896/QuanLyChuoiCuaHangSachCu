package com.oldbook.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminWebController {

    private void prepareLayout(
            Model model,
            String activeMenu,
            String pageTitle
    ) {

        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("pageTitle", pageTitle);

        /*
         * Không đưa tên/email admin vào model: trang HTML được phục vụ công khai,
         * JWT nằm trong localStorage nên request này không mang danh tính.
         * Sidebar tự điền bằng /api/profile (js/admin/layout.js).
         */
    }


    /**
     * Dashboard
     */
    @GetMapping({
            "/admin",
            "/admin/dashboard"
    })
    public String dashboardPage(Model model) {

        prepareLayout(
                model,
                "dashboard",
                "Tổng quan hệ thống"
        );

        return "admin/dashboard";
    }


    /**
     * Người dùng
     */
    @GetMapping("/admin/users")
    public String usersPage(Model model) {

        prepareLayout(
                model,
                "users",
                "Người dùng & Hồ sơ tài khoản"
        );

        return "admin/users";
    }

    @GetMapping("/admin/users/{maND}")
    public String userProfilePage(
            @PathVariable Integer maND,
            Model model
    ) {

        prepareLayout(
                model,
                "users",
                "Hồ sơ người dùng"
        );

        model.addAttribute("profileMode", "user");
        model.addAttribute("profileTargetId", maND);

        return "admin/profile";
    }


    /**
     * Tài khoản & Vai trò
     *
     * Giữ nguyên URL /admin/accounts
     * để không phá navigation cũ.
     */
    @GetMapping("/admin/accounts")
    public String accountsRolesPage(Model model) {

        prepareLayout(
                model,
                "accounts",
                "Quản lý Tài khoản & Phân quyền vai trò"
        );

        return "admin/accounts-roles";
    }

    @GetMapping("/admin/audit-logs")
    public String auditLogsPage(Model model) {

        prepareLayout(
                model,
                "audit-logs",
                "Nhật ký hệ thống & Kiểm toán hoạt động"
        );

        return "admin/audit-logs";
    }

    @GetMapping("/admin/reports")
    public String reportsPage(Model model) {

        prepareLayout(
                model,
                "reports",
                "Báo cáo doanh thu sàn"
        );

        return "admin/reports";
    }

    @GetMapping("/admin/profile")
    public String profilePage(Model model) {

        prepareLayout(
                model,
                "profile",
                "Hồ sơ Admin"
        );

        model.addAttribute("profileMode", "self");

        return "admin/profile";
    }
}