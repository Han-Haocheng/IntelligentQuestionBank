package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.dto.NameValueVO;
import com.qbank.entity.Category;
import com.qbank.entity.User;
import com.qbank.entity.WrongQuestion;
import com.qbank.interceptor.LoginInterceptor;
import com.qbank.service.CategoryService;
import com.qbank.service.WrongQuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 错题本(服务端渲染版)
 */
@Controller
public class WrongController {

    private final WrongQuestionService wrongQuestionService;
    private final CategoryService categoryService;

    public WrongController(WrongQuestionService wrongQuestionService, CategoryService categoryService) {
        this.wrongQuestionService = wrongQuestionService;
        this.categoryService = categoryService;
    }

    /** 错题列表(含题干与最近错误信息, 支持掌握状态/分类筛选) */
    @GetMapping("/wrongbook")
    public String wrongbook(@RequestParam(required = false) Integer mastered,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize,
                            Model model, HttpSession session) {
        User user = currentUser(session);
        PageInfo<WrongQuestion> pageInfo =
                wrongQuestionService.page(user.getId(), mastered, categoryId, pageNum, pageSize);
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("currentMastered", mastered);
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("categoryOptions", categoryOptions(user.getId()));
        model.addAttribute("typeNameMap", typeNameMap());
        model.addAttribute("difficultyNameMap", difficultyNameMap());
        model.addAttribute("pageTitle", "错题本");
        return "wrongbook";
    }

    /** 掌握状态切换(参考 WrongQuestionService.toggleMaster: 返回切换后状态) */
    @PostMapping("/wrongbook/master")
    public String master(@RequestParam("id") Long questionId, HttpServletRequest request,
                         RedirectAttributes ra, HttpSession session) {
        User user = currentUser(session);
        try {
            int target = wrongQuestionService.toggleMaster(user.getId(), questionId);
            ra.addFlashAttribute("flashSuccess", target == 1 ? "已标记为掌握" : "已标记为未掌握");
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return backToWrongbook(request);
    }

    /** 从错题本移除 */
    @PostMapping("/wrongbook/remove")
    public String remove(@RequestParam("id") Long questionId, HttpServletRequest request,
                         RedirectAttributes ra, HttpSession session) {
        User user = currentUser(session);
        try {
            wrongQuestionService.delete(user.getId(), questionId);
            ra.addFlashAttribute("flashSuccess", "已从错题本移除");
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return backToWrongbook(request);
    }

    // ---------- 私有辅助 ----------

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute(LoginInterceptor.SESSION_USER);
    }

    /** 分类下拉(树拍平, 子分类缩进) */
    private List<NameValueVO> categoryOptions(Long userId) {
        List<NameValueVO> options = new ArrayList<>();
        for (Category root : categoryService.tree(userId)) {
            options.add(new NameValueVO(root.getName(), root.getId()));
            if (root.getChildren() != null) {
                for (Category child : root.getChildren()) {
                    options.add(new NameValueVO("　└ " + child.getName(), child.getId()));
                }
            }
        }
        return options;
    }

    private Map<Integer, String> typeNameMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "单选题");
        map.put(2, "多选题");
        map.put(3, "填空题");
        map.put(4, "判断题");
        map.put(5, "简答题");
        return map;
    }

    private Map<Integer, String> difficultyNameMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "入门");
        map.put(2, "简单");
        map.put(3, "中等");
        map.put(4, "较难");
        map.put(5, "困难");
        return map;
    }

    /** 操作后回到错题本并尽量保留当前筛选条件 */
    private String backToWrongbook(HttpServletRequest request) {
        List<String> params = new ArrayList<>();
        String mastered = request.getParameter("mastered");
        String categoryId = request.getParameter("categoryId");
        String pageNum = request.getParameter("pageNum");
        if (StringUtils.hasText(mastered)) {
            params.add("mastered=" + mastered);
        }
        if (StringUtils.hasText(categoryId)) {
            params.add("categoryId=" + categoryId);
        }
        if (StringUtils.hasText(pageNum)) {
            params.add("pageNum=" + pageNum);
        }
        return params.isEmpty() ? "redirect:/wrongbook"
                : "redirect:/wrongbook?" + String.join("&", params);
    }
}
