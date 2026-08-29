package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.Result;
import com.qbank.dto.PracticeStartDTO;
import com.qbank.dto.PracticeStartVO;
import com.qbank.dto.PracticeSubmitDTO;
import com.qbank.entity.PracticeRecord;
import com.qbank.service.PracticeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 练习接口
 */
@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/start")
    public Result<PracticeStartVO> start(@RequestAttribute("userId") Long userId,
                                         @RequestBody PracticeStartDTO dto) {
        return Result.ok(practiceService.start(userId, dto));
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestAttribute("userId") Long userId,
                                              @RequestBody PracticeSubmitDTO dto) {
        return Result.ok(practiceService.submit(userId, dto));
    }

    /** 练习筛选条件下的总题数 */
    @GetMapping("/count")
    public Result<Integer> count(@RequestAttribute("userId") Long userId,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) Long bankId,
                                 @RequestParam(required = false) Integer difficulty,
                                 @RequestParam(required = false) Integer type,
                                 @RequestParam(required = false, defaultValue = "false") boolean onlyWrong) {
        return Result.ok(practiceService.count(userId, categoryId, bankId, difficulty, type, onlyWrong));
    }

    @GetMapping("/records")
    public Result<PageInfo<PracticeRecord>> records(@RequestAttribute("userId") Long userId,
                                                    @RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(practiceService.records(userId, pageNum, pageSize));
    }

    @GetMapping("/records/{id}")
    public Result<Map<String, Object>> detail(@RequestAttribute("userId") Long userId,
                                              @PathVariable Long id) {
        return Result.ok(practiceService.detail(userId, id));
    }

    @DeleteMapping("/records/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        practiceService.delete(userId, id);
        return Result.ok();
    }
}
