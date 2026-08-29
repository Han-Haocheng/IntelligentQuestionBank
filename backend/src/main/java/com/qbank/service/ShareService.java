package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.common.PageUtil;
import com.qbank.dto.ShareDTO;
import com.qbank.entity.Bank;
import com.qbank.entity.Question;
import com.qbank.entity.Share;
import com.qbank.entity.User;
import com.qbank.mapper.BankMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.ShareMapper;
import com.qbank.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 共享服务
 */
@Service
public class ShareService {

    private final ShareMapper shareMapper;
    private final QuestionMapper questionMapper;
    private final BankMapper bankMapper;
    private final UserMapper userMapper;

    public ShareService(ShareMapper shareMapper, QuestionMapper questionMapper,
                        BankMapper bankMapper, UserMapper userMapper) {
        this.shareMapper = shareMapper;
        this.questionMapper = questionMapper;
        this.bankMapper = bankMapper;
        this.userMapper = userMapper;
    }

    public void share(Long userId, ShareDTO dto) {
        if (dto.getBankId() != null) {
            shareBank(userId, dto);
            return;
        }
        if (dto.getQuestionId() == null) {
            throw new BusinessException("题目ID不能为空");
        }
        Question question = questionMapper.findById(dto.getQuestionId());
        if (question == null || !question.getUserId().equals(userId)) {
            throw new BusinessException("只能共享自己的题目");
        }
        int type = dto.getShareType() != null && dto.getShareType() == Constants.SHARE_TYPE_PUBLIC_QUESTION
                ? Constants.SHARE_TYPE_PUBLIC_QUESTION : Constants.SHARE_TYPE_USER_QUESTION;
        Share share = new Share();
        share.setQuestionId(dto.getQuestionId());
        share.setFromUserId(userId);
        share.setShareType(type);
        share.setPermission(permissionOf(dto, type));
        share.setMessage(dto.getMessage());
        if (type == Constants.SHARE_TYPE_USER_QUESTION) {
            if (!StringUtils.hasText(dto.getToUsername())) {
                throw new BusinessException("请输入要共享给的用户名");
            }
            User target = userMapper.findByUsername(dto.getToUsername().trim());
            if (target == null) {
                throw new BusinessException("用户不存在: " + dto.getToUsername());
            }
            if (target.getId().equals(userId)) {
                throw new BusinessException("不能共享给自己");
            }
            share.setToUserId(target.getId());
            try {
                shareMapper.insert(share);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                throw new BusinessException("该题目已共享给该用户");
            }
        } else {
            // 公开共享: 条件插入原子防重(唯一键对 NULL to_user_id 不生效)
            Share pub = new Share();
            pub.setQuestionId(dto.getQuestionId());
            pub.setFromUserId(userId);
            pub.setShareType(Constants.SHARE_TYPE_PUBLIC_QUESTION);
            pub.setPermission(Constants.PERMISSION_READ);
            pub.setMessage(dto.getMessage());
            if (shareMapper.insertPublic(pub) == 0) {
                throw new BusinessException("该题目已公开共享");
            }
        }
    }

    /** 题库共享: shareType 3=指定用户 4=公开 */
    private void shareBank(Long userId, ShareDTO dto) {
        Bank bank = bankMapper.findById(dto.getBankId());
        if (bank == null || !bank.getUserId().equals(userId)) {
            throw new BusinessException("只能共享自己的题库");
        }
        boolean isPublic = dto.getShareType() != null && dto.getShareType() == Constants.SHARE_TYPE_PUBLIC_BANK;
        Share share = new Share();
        share.setBankId(dto.getBankId());
        share.setFromUserId(userId);
        share.setShareType(isPublic ? Constants.SHARE_TYPE_PUBLIC_BANK : Constants.SHARE_TYPE_USER_BANK);
        share.setPermission(permissionOf(dto, isPublic ? Constants.SHARE_TYPE_PUBLIC_BANK : Constants.SHARE_TYPE_USER_BANK));
        share.setMessage(dto.getMessage());
        if (!isPublic) {
            if (!StringUtils.hasText(dto.getToUsername())) {
                throw new BusinessException("请输入要共享给的用户名");
            }
            User target = userMapper.findByUsername(dto.getToUsername().trim());
            if (target == null) {
                throw new BusinessException("用户不存在: " + dto.getToUsername());
            }
            if (target.getId().equals(userId)) {
                throw new BusinessException("不能共享给自己");
            }
            share.setToUserId(target.getId());
            try {
                shareMapper.insert(share);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                throw new BusinessException("该题库已共享给该用户");
            }
        } else {
            Share pub = new Share();
            pub.setBankId(dto.getBankId());
            pub.setFromUserId(userId);
            pub.setShareType(Constants.SHARE_TYPE_PUBLIC_BANK);
            pub.setMessage(dto.getMessage());
            if (shareMapper.insertPublic(pub) == 0) {
                throw new BusinessException("该题库已公开共享");
            }
        }
    }

    /** 权限解析: 公开共享固定只读; 指定用户默认只读, 可传 2 表示可编辑 */
    private int permissionOf(ShareDTO dto, int shareType) {
        if (shareType == Constants.SHARE_TYPE_PUBLIC_QUESTION || shareType == Constants.SHARE_TYPE_PUBLIC_BANK) {
            return Constants.PERMISSION_READ;
        }
        Integer p = dto.getPermission();
        return (p != null && p == Constants.PERMISSION_EDIT) ? Constants.PERMISSION_EDIT : Constants.PERMISSION_READ;
    }

    /** 修改共享权限(仅共享者; 公开共享固定只读) */
    public void updatePermission(Long fromUserId, Long shareId, Integer permission) {
        if (permission == null
                || (permission != Constants.PERMISSION_READ && permission != Constants.PERMISSION_EDIT)) {
            throw new BusinessException("权限不合法");
        }
        Share share = shareMapper.findById(shareId);
        if (share == null || !share.getFromUserId().equals(fromUserId)) {
            throw new BusinessException("共享记录不存在或无权操作");
        }
        if (share.getShareType() == Constants.SHARE_TYPE_PUBLIC_QUESTION
                || share.getShareType() == Constants.SHARE_TYPE_PUBLIC_BANK) {
            throw new BusinessException("公开共享固定只读, 无法修改权限");
        }
        shareMapper.updatePermission(shareId, permission);
    }

    public PageInfo<Share> sent(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(PageUtil.pageNum(pageNum), PageUtil.pageSize(pageSize));
        return new PageInfo<>(shareMapper.selectSent(userId));
    }

    public PageInfo<Share> received(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(PageUtil.pageNum(pageNum), PageUtil.pageSize(pageSize));
        return new PageInfo<>(shareMapper.selectReceived(userId));
    }

    public void cancel(Long userId, Long id) {
        if (shareMapper.delete(id, userId) == 0) {
            throw new BusinessException("共享记录不存在或无权取消");
        }
    }
}
