package com.qbank.controller;

import com.qbank.common.Result;
import com.qbank.dto.NameValueVO;
import com.qbank.dto.OverviewVO;
import com.qbank.dto.TrendVO;
import com.qbank.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计接口
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public Result<OverviewVO> overview(@RequestAttribute("userId") Long userId) {
        return Result.ok(statsService.overview(userId));
    }

    @GetMapping("/question-by-type")
    public Result<List<NameValueVO>> byType(@RequestAttribute("userId") Long userId) {
        return Result.ok(statsService.byType(userId));
    }

    @GetMapping("/question-by-difficulty")
    public Result<List<NameValueVO>> byDifficulty(@RequestAttribute("userId") Long userId) {
        return Result.ok(statsService.byDifficulty(userId));
    }

    @GetMapping("/question-by-category")
    public Result<List<NameValueVO>> byCategory(@RequestAttribute("userId") Long userId) {
        return Result.ok(statsService.byCategory(userId));
    }

    @GetMapping("/practice-trend")
    public Result<List<TrendVO>> trend(@RequestAttribute("userId") Long userId) {
        return Result.ok(statsService.trend(userId));
    }

    @GetMapping("/wrong-by-category")
    public Result<List<NameValueVO>> wrongByCategory(@RequestAttribute("userId") Long userId) {
        return Result.ok(statsService.wrongByCategory(userId));
    }
}
