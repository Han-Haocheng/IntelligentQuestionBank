package com.qbank.service;

import com.qbank.common.BusinessException;
import com.qbank.entity.Bank;
import com.qbank.mapper.BankMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.ShareMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 题库(题目分组)服务
 */
@Service
public class BankService {

    private final BankMapper bankMapper;
    private final QuestionMapper questionMapper;
    private final ShareMapper shareMapper;

    public BankService(BankMapper bankMapper, QuestionMapper questionMapper, ShareMapper shareMapper) {
        this.bankMapper = bankMapper;
        this.questionMapper = questionMapper;
        this.shareMapper = shareMapper;
    }

    public List<Bank> list(Long userId, Integer role) {
        List<Bank> list = new java.util.ArrayList<>();
        if (role != null && role == 0) {
            // 管理员: 查看全部用户的题库
            list.addAll(bankMapper.selectByUser(null));
        } else {
            list.addAll(bankMapper.selectByUser(userId));
            // 普通用户: 追加订阅中的共享题库
            java.util.Set<Long> seen = new java.util.HashSet<>();
            for (Bank b : list) {
                seen.add(b.getId());
            }
            for (Bank b : bankMapper.selectShared(userId)) {
                if (seen.add(b.getId())) {
                    list.add(b);
                }
            }
        }
        for (Bank bank : list) {
            bank.setQuestionCount((long) questionMapper.countByBank(bank.getId()));
        }
        return list;
    }

    public Bank get(Long userId, Long id) {
        return requireOwned(userId, id);
    }

    public void add(Long userId, Bank bank) {
        validate(bank);
        bank.setUserId(userId);
        try {
            bankMapper.insert(bank);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("已存在同名题库");
        }
    }

    public void update(Long userId, Bank bank) {
        if (bank.getId() == null) {
            throw new BusinessException("题库ID不能为空");
        }
        requireOwned(userId, bank.getId());
        validate(bank);
        Bank byName = bankMapper.findByName(userId, bank.getName());
        if (byName != null && !byName.getId().equals(bank.getId())) {
            throw new BusinessException("已存在同名题库");
        }
        bankMapper.update(bank);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        // 题目归属置空, 相关共享记录删除
        questionMapper.clearBank(id);
        shareMapper.deleteByBank(id);
        bankMapper.deleteById(id);
    }

    private Bank requireOwned(Long userId, Long id) {
        Bank bank = bankMapper.findById(id);
        if (bank == null || !bank.getUserId().equals(userId)) {
            throw new BusinessException("题库不存在或无权操作");
        }
        return bank;
    }

    private void validate(Bank bank) {
        if (!StringUtils.hasText(bank.getName())) {
            throw new BusinessException("题库名称不能为空");
        }
        if (bank.getName().length() > 50) {
            throw new BusinessException("题库名称不能超过50个字符");
        }
        if (bank.getDescription() != null && bank.getDescription().length() > 500) {
            throw new BusinessException("题库描述不能超过500个字符");
        }
    }
}
