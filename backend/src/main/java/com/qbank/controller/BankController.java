package com.qbank.controller;

import com.qbank.common.Result;
import com.qbank.entity.Bank;
import com.qbank.service.BankService;
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
 * 题库接口
 */
@RestController
@RequestMapping("/api/bank")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/list")
    public Result<List<Bank>> list(@RequestAttribute("userId") Long userId,
                                   @RequestAttribute("role") Integer role) {
        return Result.ok(bankService.list(userId, role));
    }

    @GetMapping("/{id}")
    public Result<Bank> get(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        return Result.ok(bankService.get(userId, id));
    }

    @PostMapping
    public Result<Void> add(@RequestAttribute("userId") Long userId, @RequestBody Bank bank) {
        bankService.add(userId, bank);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestAttribute("userId") Long userId, @RequestBody Bank bank) {
        bankService.update(userId, bank);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        bankService.delete(userId, id);
        return Result.ok();
    }
}
