package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.entity.WrongQuestion;
import com.qbank.mapper.WrongQuestionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 错题本服务
 */
@Service
public class WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;

    public WrongQuestionService(WrongQuestionMapper wrongQuestionMapper) {
        this.wrongQuestionMapper = wrongQuestionMapper;
    }

    public PageInfo<WrongQuestion> page(Long userId, Integer mastered, Long categoryId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<WrongQuestion> list = wrongQuestionMapper.selectPage(userId, mastered, categoryId);
        PageInfo<WrongQuestion> pageInfo = new PageInfo<>(list);
        pageInfo.setList(list);
        return pageInfo;
    }

    public int toggleMaster(Long userId, Long questionId) {
        WrongQuestion exist = wrongQuestionMapper.find(userId, questionId);
        if (exist == null) {
            throw new BusinessException("错题记录不存在");
        }
        int target = exist.getMastered() != null && exist.getMastered() == 1 ? 0 : 1;
        wrongQuestionMapper.updateMastered(userId, questionId, target);
        return target;
    }

    public void delete(Long userId, Long questionId) {
        wrongQuestionMapper.delete(userId, questionId);
    }
}
