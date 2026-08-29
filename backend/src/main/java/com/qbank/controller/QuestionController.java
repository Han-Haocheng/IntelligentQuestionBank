package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Result;
import com.qbank.dto.ImportRowDTO;
import com.qbank.dto.QuestionDTO;
import com.qbank.dto.QuestionImportSaveDTO;
import com.qbank.dto.QuestionQuery;
import com.qbank.service.QuestionImportService;
import com.qbank.service.QuestionService;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionImportService importService;

    public QuestionController(QuestionService questionService, QuestionImportService importService) {
        this.questionService = questionService;
        this.importService = importService;
    }

    @GetMapping("/list")
    public Result<PageInfo<QuestionDTO>> list(@RequestAttribute("userId") Long userId,
            @RequestAttribute("role") Integer role, QuestionQuery query) {
        return Result.ok(questionService.page(userId, role, query));
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

    // ==================== 批量导入 ====================

    /** 下载导入模板(.xlsx) */
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> template() {
        byte[] body = importService.template();
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=question-import-template.xlsx")
                .body(body);
    }

    /** 解析文件返回预览行(不落库) */
    @PostMapping("/import/parse")
    public Result<List<ImportRowDTO>> parse(@RequestParam("file") MultipartFile file) {
        try {
            return Result.ok(importService.parse(file.getOriginalFilename(), file.getInputStream()));
        } catch (java.io.IOException e) {
            throw new BusinessException("文件读取失败");
        }
    }

    /** 保存解析后的行(逐行校验, 返回成功数与失败明细) */
    @PostMapping("/import/save")
    public Result<java.util.Map<String, Object>> save(@RequestAttribute("userId") Long userId,
                                                      @RequestBody QuestionImportSaveDTO body) {
        return Result.ok(importService.save(userId, body.getRows(), body.getCategoryId(), body.getBankId()));
    }
}
