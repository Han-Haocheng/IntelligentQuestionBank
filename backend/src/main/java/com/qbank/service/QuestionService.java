package com.qbank.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.dto.QuestionDTO;
import com.qbank.dto.QuestionQuery;
import com.qbank.entity.Question;
import com.qbank.mapper.CategoryMapper;
import com.qbank.mapper.FavoriteMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.ShareMapper;
import com.qbank.mapper.WrongQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 题目服务
 */
@Service
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final CategoryMapper categoryMapper;
    private final FavoriteMapper favoriteMapper;
    private final ShareMapper shareMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionMapper questionMapper, CategoryMapper categoryMapper,
                           FavoriteMapper favoriteMapper, ShareMapper shareMapper,
                           WrongQuestionMapper wrongQuestionMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.categoryMapper = categoryMapper;
        this.favoriteMapper = favoriteMapper;
        this.shareMapper = shareMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.objectMapper = objectMapper;
    }

    public PageInfo<QuestionDTO> page(Long userId, QuestionQuery query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<Question> list = questionMapper.selectPage(userId, query);
        PageInfo<Question> pageInfo = new PageInfo<>(list);
        PageInfo<QuestionDTO> result = new PageInfo<>();
        result.setTotal(pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        result.setPages(pageInfo.getPages());
        List<QuestionDTO> dtoList = new ArrayList<>();
        for (Question question : list) {
            QuestionDTO dto = toDTO(question);
            dto.setFavorited(favoriteMapper.find(userId, question.getId()) != null);
            dtoList.add(dto);
        }
        result.setList(dtoList);
        return result;
    }

    public QuestionDTO get(Long userId, Integer role, Long id) {
        Question question = questionMapper.findById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        boolean owner = question.getUserId().equals(userId);
        if (!owner && (role == null || role != Constants.ROLE_ADMIN)
                && shareMapper.countAccessible(id, userId) == 0) {
            throw new BusinessException("无权查看该题目");
        }
        QuestionDTO dto = toDTO(question);
        dto.setFavorited(favoriteMapper.find(userId, id) != null);
        if (!owner) {
            dto.setUserId(question.getUserId());
        }
        return dto;
    }

    public void add(Long userId, QuestionDTO dto) {
        validate(dto);
        checkCategory(userId, dto.getCategoryId());
        Question question = fromDTO(dto);
        question.setUserId(userId);
        questionMapper.insert(question);
    }

    public void update(Long userId, Integer role, QuestionDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("题目ID不能为空");
        }
        Question exist = questionMapper.findById(dto.getId());
        if (exist == null) {
            throw new BusinessException("题目不存在");
        }
        if (!exist.getUserId().equals(userId) && (role == null || role != Constants.ROLE_ADMIN)) {
            throw new BusinessException("无权修改该题目");
        }
        validate(dto);
        checkCategory(exist.getUserId(), dto.getCategoryId());
        Question question = fromDTO(dto);
        question.setId(dto.getId());
        questionMapper.update(question);
    }

    public void delete(Long userId, Integer role, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的题目");
        }
        boolean admin = role != null && role == Constants.ROLE_ADMIN;
        for (Long id : ids) {
            Question exist = questionMapper.findById(id);
            if (exist == null) {
                continue;
            }
            if (!admin && !exist.getUserId().equals(userId)) {
                throw new BusinessException("无权删除题目: " + exist.getTitle());
            }
        }
        questionMapper.deleteByIds(ids);
        favoriteMapper.deleteByQuestionIds(ids);
        shareMapper.deleteByQuestionIds(ids);
        wrongQuestionMapper.deleteByQuestionIds(ids);
    }

    private void validate(QuestionDTO dto) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException("题干不能为空");
        }
        if (dto.getType() == null || dto.getType() < 1 || dto.getType() > 5) {
            throw new BusinessException("题型不合法");
        }
        if (dto.getDifficulty() == null || dto.getDifficulty() < 1 || dto.getDifficulty() > 5) {
            dto.setDifficulty(3);
        }
        if (dto.getType() == Constants.TYPE_SINGLE || dto.getType() == Constants.TYPE_MULTIPLE) {
            if (dto.getOptions() == null || dto.getOptions().size() < 2) {
                throw new BusinessException("选择题至少需要2个选项");
            }
            if (!StringUtils.hasText(dto.getAnswer())) {
                throw new BusinessException("请设置参考答案");
            }
        }
        if (dto.getType() != Constants.TYPE_SINGLE && dto.getType() != Constants.TYPE_MULTIPLE
                && !StringUtils.hasText(dto.getAnswer())) {
            throw new BusinessException("请设置参考答案");
        }
        if (dto.getType() == Constants.TYPE_SINGLE) {
            String ans = dto.getAnswer().trim().toUpperCase();
            if (ans.length() != 1 || ans.charAt(0) - 'A' >= dto.getOptions().size()) {
                throw new BusinessException("单选题答案必须是选项中的一个字母");
            }
            dto.setAnswer(ans);
        }
        if (dto.getType() == Constants.TYPE_MULTIPLE) {
            String ans = dto.getAnswer().trim().toUpperCase().replaceAll("[^A-Z]", "");
            if (ans.isEmpty()) {
                throw new BusinessException("多选题答案必须是选项字母组合");
            }
            char[] chars = ans.toCharArray();
            java.util.Arrays.sort(chars);
            String sorted = new String(chars);
            if (sorted.charAt(sorted.length() - 1) - 'A' >= dto.getOptions().size()) {
                throw new BusinessException("多选题答案超出选项范围");
            }
            dto.setAnswer(sorted);
        }
        if (dto.getType() == Constants.TYPE_JUDGE) {
            String ans = dto.getAnswer().trim();
            dto.setAnswer(ans.equals("对") || ans.equals("正确") ? "对" : "错");
        }
    }

    private void checkCategory(Long ownerId, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (categoryMapper.findById(categoryId) == null) {
            throw new BusinessException("所选分类不存在");
        }
    }

    /** options: List <-> JSON 字符串 */
    public Question fromDTO(QuestionDTO dto) {
        Question question = new Question();
        question.setCategoryId(dto.getCategoryId());
        question.setType(dto.getType());
        question.setTitle(dto.getTitle().trim());
        question.setAnswer(dto.getAnswer());
        question.setAnalysis(dto.getAnalysis());
        question.setDifficulty(dto.getDifficulty());
        question.setTags(dto.getTags());
        question.setSource(dto.getSource());
        if (dto.getType() == Constants.TYPE_SINGLE || dto.getType() == Constants.TYPE_MULTIPLE) {
            List<String> options = dto.getOptions() == null ? Collections.emptyList() : dto.getOptions();
            List<String> cleaned = new ArrayList<>();
            for (String option : options) {
                cleaned.add(option == null ? "" : option.trim());
            }
            question.setOptions(toJson(cleaned));
        }
        return question;
    }

    public QuestionDTO toDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setUserId(question.getUserId());
        dto.setCategoryId(question.getCategoryId());
        dto.setType(question.getType());
        dto.setTitle(question.getTitle());
        dto.setOptions(parseOptions(question.getOptions()));
        dto.setAnswer(question.getAnswer());
        dto.setAnalysis(question.getAnalysis());
        dto.setDifficulty(question.getDifficulty());
        dto.setTags(question.getTags());
        dto.setSource(question.getSource());
        dto.setCategoryName(question.getCategoryName());
        dto.setFavorited(question.getFavorited());
        return dto;
    }

    public List<String> parseOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toJson(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            return "[]";
        }
    }
}
