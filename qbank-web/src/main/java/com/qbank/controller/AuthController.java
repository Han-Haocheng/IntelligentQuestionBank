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
    public String loginPage(HttpServletRequest request, Model model) {
        // 已登录则直接进入首页
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginInterceptor.SESSION_USER) != null) {
            return "redirect:/";
        }
        String next = request.getParameter("next");
        if (isSafeNext(next)) {
            model.addAttribute("next", next);
        }
        model.addAttribute("pageTitle", "登录");
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginDTO dto, Model model, HttpServletRequest request) {
        try {
            User user = userService.login(dto);
            // 会话固定防护: 登录成功后轮换 SessionID (issue #9)
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            request.getSession(true).setAttribute(LoginInterceptor.SESSION_USER, user);
            String next = request.getParameter("next");
            return "redirect:" + (isSafeNext(next) ? next : "/");
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", dto.getUsername());
            String next = request.getParameter("next");
            if (isSafeNext(next)) {
                model.addAttribute("next", next);
            }
            model.addAttribute("pageTitle", "登录");
            return "login";
        }
    }

    /** 仅允许站内相对路径回跳，防开放重定向（//evil.com 或 /\\ 均拒绝） */
    private static boolean isSafeNext(String next) {
        return next != null && next.startsWith("/")
                && !next.startsWith("//") && !next.startsWith("/\\");
    }

    @GetMapping("/register")
    public String registerPage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginInterceptor.SESSION_USER) != null) {
            return "redirect:/";
        }
        model.addAttribute("pageTitle", "注册");
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterDTO dto, Model model, HttpServletRequest request) {
        try {
            User user = userService.register(dto);
            // 会话固定防护: 注册成功后同样轮换 SessionID (issue #9)
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            request.getSession(true).setAttribute(LoginInterceptor.SESSION_USER, user);
            return "redirect:/";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "注册");
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
