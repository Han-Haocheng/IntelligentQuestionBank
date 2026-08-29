package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.dto.ShareDTO;
import com.qbank.entity.Question;
import com.qbank.entity.Share;
import com.qbank.entity.User;
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
    private final UserMapper userMapper;

    public ShareService(ShareMapper shareMapper, QuestionMapper questionMapper, UserMapper userMapper) {
        this.shareMapper = shareMapper;
        this.questionMapper = questionMapper;
        this.userMapper = userMapper;
    }

    public void share(Long userId, ShareDTO dto) {
        if (dto.getQuestionId() == null) {
            throw new BusinessException("题目ID不能为空");
        }
        Question question = questionMapper.findById(dto.getQuestionId());
        if (question == null || !question.getUserId().equals(userId)) {
            throw new BusinessException("只能共享自己的题目");
        }
        int type = dto.getShareType() != null && dto.getShareType() == 2 ? 2 : 1;
        Share share = new Share();
        share.setQuestionId(dto.getQuestionId());
        share.setFromUserId(userId);
        share.setShareType(type);
        share.setMessage(dto.getMessage());
        if (type == 1) {
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
        } else {
            if (shareMapper.countPublic(dto.getQuestionId(), userId) > 0) {
                throw new BusinessException("该题目已公开共享");
            }
        }
        try {
            shareMapper.insert(share);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException("该题目已共享给该用户");
        }
    }

    public PageInfo<Share> sent(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return new PageInfo<>(shareMapper.selectSent(userId));
    }

    public PageInfo<Share> received(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return new PageInfo<>(shareMapper.selectReceived(userId));
    }

    public void cancel(Long userId, Long id) {
        if (shareMapper.delete(id, userId) == 0) {
            throw new BusinessException("共享记录不存在或无权取消");
        }
    }
}
