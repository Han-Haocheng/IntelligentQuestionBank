package com.qbank.controller;

import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.entity.Category;
import com.qbank.entity.User;
import com.qbank.interceptor.LoginInterceptor;
import com.qbank.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类管理(服务端渲染版)
 * 分类为全局共享(所有用户可见), 仅管理员(role==0)可新增/修改/删除。
 */
@Controller
public class CategoryController {

    private static final String REDIRECT_LIST = "redirect:/categories";

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public String list(HttpSession session, Model model) {
        User user = currentUser(session);
        // 树形数据: 顶级分类(含 children)
        List<Category> tree = categoryService.tree(user.getId());
        // 顶级分类列表(parentId == 0), 供新增/编辑时选择上级分类(二级树只允许顶级作父级)
        List<Category> roots = categoryService.list(user.getId()).stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0L)
                .collect(Collectors.toList());
        model.addAttribute("tree", tree);
        model.addAttribute("roots", roots);
        model.addAttribute("isAdmin", isAdmin(user));
        return "categories";
    }

    @PostMapping("/categories/add")
    public String add(@RequestParam String name,
                      @RequestParam(required = false, defaultValue = "0") Long parentId,
                      @RequestParam(required = false) Integer sort,
                      HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        if (!isAdmin(user)) {
            return noPermission(ra);
        }
        Category category = new Category();
        category.setName(name == null ? null : name.trim());
        category.setParentId(parentId);
        category.setSort(sort);
        try {
            categoryService.add(user.getId(), category);
            ra.addFlashAttribute("flashSuccess", "新增分类成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/categories/update")
    public String update(@RequestParam Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) Long parentId,
                         @RequestParam(required = false) Integer sort,
                         HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        if (!isAdmin(user)) {
            return noPermission(ra);
        }
        Category category = new Category();
        category.setId(id);
        category.setName(name == null ? null : name.trim());
        category.setParentId(parentId);
        category.setSort(sort);
        try {
            categoryService.update(user.getId(), category);
            ra.addFlashAttribute("flashSuccess", "修改分类成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/categories/delete")
    public String delete(@RequestParam Long id, HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        if (!isAdmin(user)) {
            return noPermission(ra);
        }
        try {
            categoryService.delete(user.getId(), id);
            ra.addFlashAttribute("flashSuccess", "删除分类成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return REDIRECT_LIST;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute(LoginInterceptor.SESSION_USER);
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() != null && user.getRole() == Constants.ROLE_ADMIN;
    }

    private String noPermission(RedirectAttributes ra) {
        ra.addFlashAttribute("flashError", "无权限操作, 仅管理员可维护分类");
        return REDIRECT_LIST;
    }
}
