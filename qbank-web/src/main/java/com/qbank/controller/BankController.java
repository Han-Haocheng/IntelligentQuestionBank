package com.qbank.controller;

import com.qbank.common.BusinessException;
import com.qbank.entity.Bank;
import com.qbank.entity.User;
import com.qbank.interceptor.LoginInterceptor;
import com.qbank.service.BankService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 题库管理(服务端渲染版)
 * 题库仅本人可管理(新增/修改/删除), 归属校验由 BankService 完成;
 * 删除题库时题目仅解绑保留(service 处理)。
 */
@Controller
public class BankController {

    private static final String REDIRECT_LIST = "redirect:/banks";

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/banks")
    public String list(HttpSession session, Model model) {
        User user = currentUser(session);
        // 管理员看到全部用户题库; 普通用户看到自己的 + 订阅中的共享题库; 均含题目计数
        model.addAttribute("banks", bankService.list(user.getId(), user.getRole()));
        return "banks";
    }

    @PostMapping("/banks/add")
    public String add(@RequestParam String name,
                      @RequestParam(required = false) String description,
                      HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        Bank bank = new Bank();
        bank.setName(name == null ? null : name.trim());
        bank.setDescription(normalize(description));
        try {
            bankService.add(user.getId(), bank);
            ra.addFlashAttribute("flashSuccess", "新增题库成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/banks/update")
    public String update(@RequestParam Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        Bank bank = new Bank();
        bank.setId(id);
        bank.setName(name == null ? null : name.trim());
        bank.setDescription(normalize(description));
        try {
            bankService.update(user.getId(), bank);
            ra.addFlashAttribute("flashSuccess", "修改题库成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/banks/delete")
    public String delete(@RequestParam Long id, HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        try {
            bankService.delete(user.getId(), id);
            ra.addFlashAttribute("flashSuccess", "删除题库成功(库内题目已保留并解绑)");
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return REDIRECT_LIST;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute(LoginInterceptor.SESSION_USER);
    }

    private String normalize(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
