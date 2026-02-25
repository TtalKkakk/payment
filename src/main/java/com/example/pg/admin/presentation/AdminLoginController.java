package com.example.pg.admin.presentation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 관리자 로그인 화면.
 * POST /admin/login 은 Spring Security가 처리한다.
 */
@Controller
@RequestMapping("/admin")
public class AdminLoginController {

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        model.addAttribute("_csrf", request.getAttribute("_csrf"));
        return "admin/login";
    }
}
