package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.entity.Favorite;
import com.qbank.entity.Question;
import com.qbank.mapper.FavoriteMapper;
import com.qbank.mapper.QuestionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 收藏服务
 */
@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final QuestionMapper questionMapper;

    public FavoriteService(FavoriteMapper favoriteMapper, QuestionMapper questionMapper) {
        this.favoriteMapper = favoriteMapper;
        this.questionMapper = questionMapper;
    }

    public boolean toggle(Long userId, Long questionId) {
        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        Favorite exist = favoriteMapper.find(userId, questionId);
        if (exist != null) {
            favoriteMapper.delete(userId, questionId);
            return false;
        }
        favoriteMapper.insert(userId, questionId);
        return true;
    }

    public PageInfo<Question> page(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return new PageInfo<>(favoriteMapper.selectPage(userId));
    }

    public void remove(Long userId, Long questionId) {
        favoriteMapper.delete(userId, questionId);
    }
}
