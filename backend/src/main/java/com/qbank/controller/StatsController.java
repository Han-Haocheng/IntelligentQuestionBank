package com.qbank.controller;

import com.qbank.common.Result;
import com.qbank.dto.NameValueVO;
import com.qbank.dto.OverviewVO;
import com.qbank.dto.TrendVO;
import com.qbank.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计接口 (管理员可传 targetUserId 查看指定用户, 不传则统计全部)
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public Result<OverviewVO> overview(@RequestAttribute("userId") Long userId,
                                       @RequestAttribute("role") Integer role,
                                       @RequestParam(required = false) Long targetUserId) {
        return Result.ok(statsService.overview(userId, role, targetUserId));
    }

    @GetMapping("/question-by-type")
    public Result<List<NameValueVO>> byType(@RequestAttribute("userId") Long userId,
                                            @RequestAttribute("role") Integer role,
                                            @RequestParam(required = false) Long targetUserId) {
        return Result.ok(statsService.byType(userId, role, targetUserId));
    }

    @GetMapping("/question-by-difficulty")
    public Result<List<NameValueVO>> byDifficulty(@RequestAttribute("userId") Long userId,
                                                  @RequestAttribute("role") Integer role,
                                                  @RequestParam(required = false) Long targetUserId) {
        return Result.ok(statsService.byDifficulty(userId, role, targetUserId));
    }

    @GetMapping("/question-by-category")
    public Result<List<NameValueVO>> byCategory(@RequestAttribute("userId") Long userId,
                                                @RequestAttribute("role") Integer role,
                                                @RequestParam(required = false) Long targetUserId) {
        return Result.ok(statsService.byCategory(userId, role, targetUserId));
    }

    @GetMapping("/practice-trend")
    public Result<List<TrendVO>> trend(@RequestAttribute("userId") Long userId,
                                       @RequestAttribute("role") Integer role,
                                       @RequestParam(required = false) Long targetUserId) {
        return Result.ok(statsService.trend(userId, role, targetUserId));
    }

    @GetMapping("/wrong-by-category")
    public Result<List<NameValueVO>> wrongByCategory(@RequestAttribute("userId") Long userId,
                                                     @RequestAttribute("role") Integer role,
                                                     @RequestParam(required = false) Long targetUserId) {
        return Result.ok(statsService.wrongByCategory(userId, role, targetUserId));
    }
}
