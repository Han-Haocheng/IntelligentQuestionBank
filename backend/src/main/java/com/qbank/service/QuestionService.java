package com.qbank.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.dto.QuestionDTO;
import com.qbank.dto.QuestionQuery;
import com.qbank.entity.Category;
import com.qbank.entity.Question;
import com.qbank.mapper.BankMapper;
import com.qbank.mapper.CategoryMapper;
import com.qbank.mapper.FavoriteMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.ShareMapper;
import com.qbank.mapper.WrongQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 题目服务
 */
@Service
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final CategoryMapper categoryMapper;
    private final BankMapper bankMapper;
    private final FavoriteMapper favoriteMapper;
    private final ShareMapper shareMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionMapper questionMapper, CategoryMapper categoryMapper,
                           BankMapper bankMapper, FavoriteMapper favoriteMapper, ShareMapper shareMapper,
                           WrongQuestionMapper wrongQuestionMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.categoryMapper = categoryMapper;
        this.bankMapper = bankMapper;
        this.favoriteMapper = favoriteMapper;
        this.shareMapper = shareMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.objectMapper = objectMapper;
    }

    public PageInfo<QuestionDTO> page(Long userId, Integer role, QuestionQuery query) {
        Long scope = userId;
        if (role != null && role == Constants.ROLE_ADMIN) {
            // 管理员: 传 userId 查看指定用户, 不传查看全部
            scope = query.getUserId() != null ? query.getUserId() : null;
        } else {
            scope = scopeUserId(userId, query.getBankId());
        }
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<Question> list = questionMapper.selectPage(scope, query);
        PageInfo<Question> pageInfo = new PageInfo<>(list);
        PageInfo<QuestionDTO> result = new PageInfo<>();
        result.setTotal(pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        result.setPages(pageInfo.getPages());
        // 批量查询本页题目的收藏状态, 避免每行一次 find 查询(N+1)
        Set<Long> favoritedIds = new HashSet<>();
        if (!list.isEmpty()) {
            List<Long> pageIds = new ArrayList<>();
            for (Question question : list) {
                pageIds.add(question.getId());
            }
            favoritedIds.addAll(favoriteMapper.selectIdsByUserAndQuestionIds(userId, pageIds));
        }
        List<QuestionDTO> dtoList = new ArrayList<>();
        for (Question question : list) {
            QuestionDTO dto = toDTO(question);
            dto.setFavorited(favoritedIds.contains(question.getId()));
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
        // 允许加入自己或"可编辑共享"的题库; 新题目归属题库所有者(写回共享者空间)
        Long ownerId = resolveBankOwner(userId, dto.getBankId());
        Question question = fromDTO(dto);
        question.setUserId(ownerId);
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
        boolean owner = exist.getUserId().equals(userId);
        boolean admin = role != null && role == Constants.ROLE_ADMIN;
        boolean sharedEdit = shareMapper.countEditable(exist.getId(), userId) > 0
                || (exist.getBankId() != null && shareMapper.countBankEditable(exist.getBankId(), userId) > 0);
        if (!owner && !admin && !sharedEdit) {
            throw new BusinessException("无权修改该题目");
        }
        validate(dto);
        checkCategory(exist.getUserId(), dto.getCategoryId());
        checkBankOwner(exist.getUserId(), dto.getBankId());
        if (!owner && !admin) {
            // 共享编辑者: 仅可改内容, 不可改分类/题库归属
            dto.setCategoryId(exist.getCategoryId());
            dto.setBankId(exist.getBankId());
        }
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

    /** 全局分类: 仅校验存在(分类由管理员维护, 所有用户共用) */
    private void checkCategory(Long ownerId, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (categoryMapper.findById(categoryId) == null) {
            throw new BusinessException("所选分类不存在");
        }
    }

    /** 供练习等场景复用: 校验题库共享作用域后返回查询用户 */
    public Long resolvePracticeScope(Long userId, Long bankId) {
        return scopeUserId(userId, bankId);
    }

    /**
     * 计算题目查询作用域: 传了 bankId 且该库属于他人时,
     * 校验是否收到过该题库的共享, 通过则改按库归属者查询
     */
    private Long scopeUserId(Long userId, Long bankId) {
        if (bankId == null) {
            return userId;
        }
        com.qbank.entity.Bank bank = bankMapper.findById(bankId);
        if (bank == null || bank.getUserId().equals(userId)) {
            return userId;
        }
        if (shareMapper.countBankAccessible(bankId, userId) == 0) {
            throw new BusinessException("无权查看该题库");
        }
        return bank.getUserId();
    }

    /** 更新场景: 题库必须属于题目所有者(共享编辑者不可改题库归属) */
    private void checkBankOwner(Long ownerId, Long bankId) {
        if (bankId == null) {
            return;
        }
        com.qbank.entity.Bank bank = bankMapper.findById(bankId);
        if (bank == null || !bank.getUserId().equals(ownerId)) {
            throw new BusinessException("所选题库不存在或无权使用");
        }
    }

    /** 新增场景: 本人题库或"可编辑共享"题库可用, 返回题库所有者 */
    private Long resolveBankOwner(Long userId, Long bankId) {
        if (bankId == null) {
            return userId;
        }
        com.qbank.entity.Bank bank = bankMapper.findById(bankId);
        if (bank == null) {
            throw new BusinessException("所选题库不存在或无权使用");
        }
        if (!bank.getUserId().equals(userId) && shareMapper.countBankEditable(bankId, userId) == 0) {
            throw new BusinessException("所选题库不存在或无权使用");
        }
        return bank.getUserId();
    }

    /** options: List <-> JSON 字符串 */
    public Question fromDTO(QuestionDTO dto) {
        Question question = new Question();
        question.setCategoryId(dto.getCategoryId());
        question.setBankId(dto.getBankId());
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
        dto.setBankId(question.getBankId());
        dto.setType(question.getType());
        dto.setTitle(question.getTitle());
        dto.setOptions(parseOptions(question.getOptions()));
        dto.setAnswer(question.getAnswer());
        dto.setAnalysis(question.getAnalysis());
        dto.setDifficulty(question.getDifficulty());
        dto.setTags(question.getTags());
        dto.setSource(question.getSource());
        dto.setCategoryName(question.getCategoryName());
        dto.setBankName(question.getBankName());
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
