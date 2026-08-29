package com.qbank.service;

import com.qbank.common.Constants;
import com.qbank.dto.NameValueVO;
import com.qbank.dto.OverviewVO;
import com.qbank.dto.TrendVO;
import com.qbank.entity.PracticeRecord;
import com.qbank.mapper.CategoryMapper;
import com.qbank.mapper.FavoriteMapper;
import com.qbank.mapper.PracticeRecordMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.UserMapper;
import com.qbank.mapper.WrongQuestionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务
 */
@Service
public class StatsService {

    private final QuestionMapper questionMapper;
    private final CategoryMapper categoryMapper;
    private final FavoriteMapper favoriteMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final UserMapper userMapper;

    public StatsService(QuestionMapper questionMapper, CategoryMapper categoryMapper,
                        FavoriteMapper favoriteMapper, WrongQuestionMapper wrongQuestionMapper,
                        PracticeRecordMapper practiceRecordMapper, UserMapper userMapper) {
        this.questionMapper = questionMapper;
        this.categoryMapper = categoryMapper;
        this.favoriteMapper = favoriteMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.userMapper = userMapper;
    }

    /**
     * 统计作用域: 管理员可传 targetUserId 查看指定用户, 不传则统计全部用户; 普通用户仅本人
     */
    private Long scope(Long userId, Integer role, Long targetUserId) {
        if (role != null && role == Constants.ROLE_ADMIN) {
            return targetUserId;  // null = 全部
        }
        return userId;
    }

    public OverviewVO overview(Long userId, Integer role, Long targetUserId) {
        Long scope = scope(userId, role, targetUserId);
        OverviewVO vo = new OverviewVO();
        vo.setQuestionCount((long) questionMapper.countByUser(scope));
        vo.setCategoryCount((long) categoryMapper.countAll());
        vo.setFavoriteCount((long) favoriteMapper.countByUser(scope));
        vo.setWrongCount((long) wrongQuestionMapper.countByUser(scope, null));
        vo.setPracticeCount((long) practiceRecordMapper.countByUser(scope));
        PracticeRecord sum = practiceRecordMapper.selectSumStats(scope);
        long total = sum == null || sum.getTotal() == null ? 0 : sum.getTotal();
        long correct = sum == null || sum.getCorrect() == null ? 0 : sum.getCorrect();
        vo.setAccuracy(total == 0 ? 0.0 : Math.round(correct * 1000.0 / total) / 10.0);
        if (role != null && role == Constants.ROLE_ADMIN) {
            vo.setUserCount((long) userMapper.countByUser(null));
        }
        return vo;
    }

    public List<NameValueVO> byType(Long userId, Integer role, Long targetUserId) {
        List<NameValueVO> raw = questionMapper.countByType(scope(userId, role, targetUserId));
        Map<String, Long> map = toMap(raw);
        List<NameValueVO> result = new ArrayList<>();
        for (int i = 0; i < Constants.TYPE_NAMES.size(); i++) {
            Long count = map.get(String.valueOf(i + 1));
            if (count != null) {
                result.add(new NameValueVO(Constants.TYPE_NAMES.get(i), count));
            }
        }
        return result;
    }

    public List<NameValueVO> byDifficulty(Long userId, Integer role, Long targetUserId) {
        List<NameValueVO> raw = questionMapper.countByDifficulty(scope(userId, role, targetUserId));
        Map<String, Long> map = toMap(raw);
        List<NameValueVO> result = new ArrayList<>();
        for (int i = 0; i < Constants.DIFFICULTY_NAMES.size(); i++) {
            Long count = map.get(String.valueOf(i + 1));
            if (count != null) {
                result.add(new NameValueVO(Constants.DIFFICULTY_NAMES.get(i), count));
            }
        }
        return result;
    }

    public List<NameValueVO> byCategory(Long userId, Integer role, Long targetUserId) {
        return questionMapper.countByCategory(scope(userId, role, targetUserId));
    }

    public List<NameValueVO> wrongByCategory(Long userId, Integer role, Long targetUserId) {
        return wrongQuestionMapper.countGroupByCategory(scope(userId, role, targetUserId));
    }

    /** 近14天练习趋势(补零) */
    public List<TrendVO> trend(Long userId, Integer role, Long targetUserId) {
        Long scope = scope(userId, role, targetUserId);
        Map<String, TrendVO> byDate = new HashMap<>();
        for (TrendVO item : practiceRecordMapper.selectTrend(scope)) {
            byDate.put(item.getDate(), item);
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<TrendVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 13; i >= 0; i--) {
            String date = today.minusDays(i).format(fmt);
            TrendVO item = byDate.get(date);
            if (item == null) {
                result.add(new TrendVO(date, 0L, 0L, 0.0));
            } else {
                long total = item.getTotal() == null ? 0 : item.getTotal();
                long correct = item.getCorrect() == null ? 0 : item.getCorrect();
                double accuracy = total == 0 ? 0.0 : Math.round(correct * 1000.0 / total) / 10.0;
                result.add(new TrendVO(date, total, correct, accuracy));
            }
        }
        return result;
    }

    private Map<String, Long> toMap(List<NameValueVO> raw) {
        Map<String, Long> map = new HashMap<>();
        if (raw != null) {
            for (NameValueVO item : raw) {
                map.put(item.getName(), item.getValue());
            }
        }
        return map;
    }
}
