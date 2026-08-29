package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.Result;
import com.qbank.dto.ShareDTO;
import com.qbank.entity.Share;
import com.qbank.service.ShareService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共享接口
 */
@RestController
@RequestMapping("/api/share")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping
    public Result<Void> share(@RequestAttribute("userId") Long userId, @RequestBody ShareDTO dto) {
        shareService.share(userId, dto);
        return Result.ok();
    }

    @GetMapping("/sent")
    public Result<PageInfo<Share>> sent(@RequestAttribute("userId") Long userId,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(shareService.sent(userId, pageNum, pageSize));
    }

    @GetMapping("/received")
    public Result<PageInfo<Share>> received(@RequestAttribute("userId") Long userId,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(shareService.received(userId, pageNum, pageSize));
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        shareService.cancel(userId, id);
        return Result.ok();
    }
}
