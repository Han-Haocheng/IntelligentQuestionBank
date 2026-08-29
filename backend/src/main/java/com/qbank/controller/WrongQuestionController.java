package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.Result;
import com.qbank.entity.WrongQuestion;
import com.qbank.service.WrongQuestionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 错题本接口
 */
@RestController
@RequestMapping("/api/wrong")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    @GetMapping("/list")
    public Result<PageInfo<WrongQuestion>> list(@RequestAttribute("userId") Long userId,
                                                @RequestParam(required = false) Integer mastered,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(wrongQuestionService.page(userId, mastered, categoryId, pageNum, pageSize));
    }

    @PutMapping("/master/{questionId}")
    public Result<Integer> toggleMaster(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long questionId) {
        // 返回切换后的状态: 1已掌握 0未掌握
        return Result.ok(wrongQuestionService.toggleMaster(userId, questionId));
    }

    @DeleteMapping("/{questionId}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId, @PathVariable Long questionId) {
        wrongQuestionService.delete(userId, questionId);
        return Result.ok();
    }
}
