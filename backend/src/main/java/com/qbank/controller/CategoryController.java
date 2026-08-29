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

    /** 同级拖拽排序(管理员): body {"parentId": N, "ids": [按新顺序排列的分类id]} */
    @PostMapping("/sort")
    public Result<Void> sort(@RequestAttribute("role") Integer role,
                             @RequestBody java.util.Map<String, Object> body) {
        requireAdmin(role);
        Long parentId = body == null || body.get("parentId") == null ? null
                : ((Number) body.get("parentId")).longValue();
        java.util.List<Long> ids = new java.util.ArrayList<>();
        if (body != null && body.get("ids") instanceof java.util.List<?> list) {
            for (Object v : list) {
                if (v != null) {
                    ids.add(((Number) v).longValue());
                }
            }
        }
        categoryService.sort(parentId, ids);
        return Result.ok();
    }

    /** 合并分类(管理员): 把该分类及子级的题目迁移到 targetId, 返回迁移数量 */
    @PostMapping("/{id}/merge")
    public Result<Integer> merge(@RequestAttribute("role") Integer role,
                                 @PathVariable Long id,
                                 @RequestBody java.util.Map<String, Object> body) {
        requireAdmin(role);
        Long targetId = body == null || body.get("targetId") == null ? null
                : ((Number) body.get("targetId")).longValue();
        return Result.ok(categoryService.merge(id, targetId));
    }

    /** 分类影响面统计(任意登录用户): 该分类及子级题目数/子分类数 */
    @GetMapping("/{id}/count")
    public Result<java.util.Map<String, Object>> count(@PathVariable Long id) {
        return Result.ok(categoryService.count(id));
    }

    private void requireAdmin(Integer role) {
        if (role == null || role != 0) {
            throw new com.qbank.common.BusinessException("无权限操作");
        }
    }
}
