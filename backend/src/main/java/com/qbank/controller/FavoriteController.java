package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.Result;
import com.qbank.entity.Question;
import com.qbank.service.FavoriteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收藏接口
 */
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{questionId}/toggle")
    public Result<Boolean> toggle(@RequestAttribute("userId") Long userId, @PathVariable Long questionId) {
        // true=已收藏, false=已取消
        return Result.ok(favoriteService.toggle(userId, questionId));
    }

    @GetMapping("/list")
    public Result<PageInfo<Question>> list(@RequestAttribute("userId") Long userId,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(favoriteService.page(userId, pageNum, pageSize));
    }

    @DeleteMapping("/{questionId}")
    public Result<Void> remove(@RequestAttribute("userId") Long userId, @PathVariable Long questionId) {
        favoriteService.remove(userId, questionId);
        return Result.ok();
    }
}
