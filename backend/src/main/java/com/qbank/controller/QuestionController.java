package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.Result;
import com.qbank.dto.QuestionDTO;
import com.qbank.dto.QuestionQuery;
import com.qbank.service.QuestionService;
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
 * 题目接口
 */
@RestController
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/list")
    public Result<PageInfo<QuestionDTO>> list(@RequestAttribute("userId") Long userId, QuestionQuery query) {
        return Result.ok(questionService.page(userId, query));
    }

    @GetMapping("/{id}")
    public Result<QuestionDTO> get(@RequestAttribute("userId") Long userId,
                                   @RequestAttribute("role") Integer role,
                                   @PathVariable Long id) {
        return Result.ok(questionService.get(userId, role, id));
    }

    @PostMapping
    public Result<Void> add(@RequestAttribute("userId") Long userId, @RequestBody QuestionDTO dto) {
        questionService.add(userId, dto);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestAttribute("userId") Long userId,
                               @RequestAttribute("role") Integer role,
                               @RequestBody QuestionDTO dto) {
        questionService.update(userId, role, dto);
        return Result.ok();
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @RequestAttribute("role") Integer role,
                               @RequestBody List<Long> ids) {
        questionService.delete(userId, role, ids);
        return Result.ok();
    }
}
