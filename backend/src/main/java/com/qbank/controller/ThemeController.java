package com.qbank.controller;

import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.common.Result;
import com.qbank.entity.Theme;
import com.qbank.service.ThemeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前端样式主题接口: 增删改/启停/设默认仅管理员, 查询类全员可用
 */
@RestController
@RequestMapping("/api/theme")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    /** 当前全局生效主题(无需登录, 登录页/应用启动时拉取) */
    @GetMapping("/active")
    public Result<Theme> active() {
        return Result.ok(themeService.active());
    }

    /** 启用的主题列表(登录用户用于切换样式) */
    @GetMapping("/enabled")
    public Result<List<Theme>> enabled() {
        return Result.ok(themeService.enabled());
    }

    // ==================== 管理员接口 ====================

    @GetMapping("/list")
    public Result<List<Theme>> list(@RequestAttribute("role") Integer role) {
        requireAdmin(role);
        return Result.ok(themeService.list());
    }

    @PostMapping
    public Result<Theme> add(@RequestAttribute("role") Integer role, @RequestBody Theme theme) {
        requireAdmin(role);
        return Result.ok(themeService.add(theme));
    }

    @PutMapping
    public Result<Theme> update(@RequestAttribute("role") Integer role, @RequestBody Theme theme) {
        requireAdmin(role);
        return Result.ok(themeService.update(theme));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@RequestAttribute("role") Integer role,
                                     @PathVariable Long id, @RequestParam Integer enabled) {
        requireAdmin(role);
        themeService.updateStatus(id, enabled);
        return Result.ok();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@RequestAttribute("role") Integer role, @PathVariable Long id) {
        requireAdmin(role);
        themeService.setDefault(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("role") Integer role, @PathVariable Long id) {
        requireAdmin(role);
        themeService.delete(id);
        return Result.ok();
    }

    private void requireAdmin(Integer role) {
        if (role == null || role != Constants.ROLE_ADMIN) {
            throw new BusinessException("无权限操作");
        }
    }
}