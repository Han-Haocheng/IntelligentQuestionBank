package com.qbank.controller;

import com.qbank.common.BusinessException;
import com.qbank.dto.LoginDTO;
import com.qbank.dto.RegisterDTO;
import com.qbank.entity.User;
import com.qbank.interceptor.LoginInterceptor;
import com.qbank.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 登录/注册(服务端渲染版, Session 会话)
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        // 已登录则直接进入首页
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginInterceptor.SESSION_USER) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginDTO dto, Model model, HttpServletRequest request) {
        try {
            User user = userService.login(dto);
            request.getSession().setAttribute(LoginInterceptor.SESSION_USER, user);
            return "redirect:/";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginInterceptor.SESSION_USER) != null) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterDTO dto, Model model, HttpServletRequest request) {
        try {
            User user = userService.register(dto);
            request.getSession().setAttribute(LoginInterceptor.SESSION_USER, user);
            return "redirect:/";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
