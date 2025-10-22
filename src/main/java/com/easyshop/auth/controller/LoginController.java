package com.easyshop.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            HttpServletRequest request,
                            Model model) {
        if (error != null) {
            model.addAttribute("loginErrorCode", error);
        }
        // Pre-fill last attempted username for better UX and OTP modal flow
        try {
            var session = request.getSession(false);
            Object lastUsername = session != null ? session.getAttribute("SPRING_SECURITY_LAST_USERNAME") : null;
            if (lastUsername instanceof String s && !s.isBlank()) {
                model.addAttribute("lastUsername", s);
            }
        } catch (Exception ignored) {
        }
        return "login";
    }
}
