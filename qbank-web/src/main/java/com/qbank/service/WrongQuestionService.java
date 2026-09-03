package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.PageUtil;
import com.qbank.entity.WrongQuestion;
import com.qbank.mapper.FavoriteMapper;
import com.qbank.mapper.WrongQuestionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 错题本服务
 */
@Service
public class WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final FavoriteMapper favoriteMapper;

    public WrongQuestionService(WrongQuestionMapper wrongQuestionMapper, FavoriteMapper favoriteMapper) {
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.favoriteMapper = favoriteMapper;
    }

    public PageInfo<WrongQuestion> page(Long userId, Integer mastered, Long categoryId, int pageNum, int pageSize) {
        PageHelper.startPage(PageUtil.pageNum(pageNum), PageUtil.pageSize(pageSize));
        List<WrongQuestion> list = wrongQuestionMapper.selectPage(userId, mastered, categoryId);
        PageInfo<WrongQuestion> pageInfo = new PageInfo<>(list);
        pageInfo.setList(list);
        // 批量查询本页题目的收藏状态, 避免每行一次查询(N+1)
        if (list != null && !list.isEmpty()) {
            List<Long> questionIds = new ArrayList<>();
            for (WrongQuestion w : list) {
                questionIds.add(w.getQuestionId());
            }
            Set<Long> favoritedIds = new HashSet<>(favoriteMapper.selectIdsByUserAndQuestionIds(userId, questionIds));
            for (WrongQuestion w : list) {
                w.setFavorited(favoritedIds.contains(w.getQuestionId()));
            }
        }
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
