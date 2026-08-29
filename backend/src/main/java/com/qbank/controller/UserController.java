package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.common.Result;
import com.qbank.dto.LoginDTO;
import com.qbank.dto.LoginVO;
import com.qbank.dto.RegisterDTO;
import com.qbank.dto.UserUpdateDTO;
import com.qbank.entity.User;
import com.qbank.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        userService.logout((String) request.getAttribute("token"));
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<User> info(@RequestAttribute("userId") Long userId) {
        return Result.ok(userService.info(userId));
    }

    @PutMapping("/update")
    public Result<User> update(@RequestAttribute("userId") Long userId, @RequestBody UserUpdateDTO dto) {
        return Result.ok(userService.update(userId, dto));
    }

    // ==================== 管理员接口 ====================

    @PostMapping("/add")
    public Result<User> add(@RequestAttribute("role") Integer role, @RequestBody RegisterDTO dto) {
        requireAdmin(role);
        return Result.ok(userService.add(dto));
    }

    @GetMapping("/list")
    public Result<PageInfo<User>> list(@RequestAttribute("role") Integer role,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        requireAdmin(role);
        return Result.ok(userService.page(keyword, pageNum, pageSize));
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestAttribute("userId") Long operatorId,
                                     @RequestAttribute("role") Integer role,
                                     @RequestParam Long id, @RequestParam Integer status) {
        requireAdmin(role);
        userService.updateStatus(operatorId, id, status);
        return Result.ok();
    }

    @PutMapping("/reset-password/{id}")
    public Result<Void> resetPassword(@RequestAttribute("userId") Long operatorId,
                                      @RequestAttribute("role") Integer role,
                                      @PathVariable Long id) {
        requireAdmin(role);
        userService.resetPassword(operatorId, id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long operatorId,
                               @RequestAttribute("role") Integer role,
                               @PathVariable Long id) {
        requireAdmin(role);
        userService.delete(operatorId, id);
        return Result.ok();
    }

    private void requireAdmin(Integer role) {
        if (role == null || role != Constants.ROLE_ADMIN) {
            throw new BusinessException("无权限操作");
        }
    }
}
