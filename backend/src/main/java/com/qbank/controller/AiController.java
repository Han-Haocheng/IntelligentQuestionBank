package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.Result;
import com.qbank.dto.AiResultVO;
import com.qbank.entity.AiAnalysis;
import com.qbank.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 分析接口
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/analyze/question/{id}")
    public Result<AiResultVO> analyzeQuestion(@RequestAttribute("userId") Long userId,
                                              @PathVariable Long id) {
        return Result.ok(aiService.analyzeQuestion(userId, id));
    }

    @PostMapping("/analyze/report")
    public Result<AiResultVO> report(@RequestAttribute("userId") Long userId) {
        return Result.ok(aiService.report(userId));
    }

    @GetMapping("/history")
    public Result<PageInfo<AiAnalysis>> history(@RequestAttribute("userId") Long userId,
                                                @RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(aiService.history(userId, pageNum, pageSize));
    }
}
