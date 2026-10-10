package com.oldbook.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthWebController {

    @GetMapping("/login")
    public String loginPage(Model model) {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        return "auth/register";
    }

    @GetMapping("/verify-register")
    public String verifyRegisterPage(Model model) {
        return "auth/verify-register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        return "auth/forgot-password";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        return "profile";
    }

    @GetMapping("/addresses")
    public String addressesPage(Model model) {
        return "addresses";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        return "auth/change-password";
    }
}