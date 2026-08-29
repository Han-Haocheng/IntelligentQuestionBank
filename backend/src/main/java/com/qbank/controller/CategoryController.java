package com.qbank.controller;

import com.qbank.common.Result;
import com.qbank.entity.Category;
import com.qbank.service.CategoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    public Result<List<Category>> tree(@RequestAttribute("userId") Long userId) {
        return Result.ok(categoryService.tree(userId));
    }

    @GetMapping("/list")
    public Result<List<Category>> list(@RequestAttribute("userId") Long userId) {
        return Result.ok(categoryService.list(userId));
    }

    @PostMapping
    public Result<Void> add(@RequestAttribute("userId") Long userId,
                            @RequestAttribute("role") Integer role,
                            @RequestBody Category category) {
        requireAdmin(role);
        categoryService.add(userId, category);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestAttribute("userId") Long userId,
                               @RequestAttribute("role") Integer role,
                               @RequestBody Category category) {
        requireAdmin(role);
        categoryService.update(userId, category);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @RequestAttribute("role") Integer role,
                               @PathVariable Long id) {
        requireAdmin(role);
        categoryService.delete(userId, id);
        return Result.ok();
    }

    private void requireAdmin(Integer role) {
        if (role == null || role != 0) {
            throw new com.qbank.common.BusinessException("无权限操作");
        }
    }
}
