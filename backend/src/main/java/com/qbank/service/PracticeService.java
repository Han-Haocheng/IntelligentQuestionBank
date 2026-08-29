package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.dto.PracticeStartDTO;
import com.qbank.dto.PracticeStartVO;
import com.qbank.dto.PracticeSubmitDTO;
import com.qbank.dto.QuestionDTO;
import com.qbank.entity.PracticeAnswer;
import com.qbank.entity.PracticeRecord;
import com.qbank.entity.Question;
import com.qbank.entity.WrongQuestion;
import com.qbank.mapper.PracticeAnswerMapper;
import com.qbank.mapper.PracticeRecordMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.WrongQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 练习服务: 开始练习(隐藏答案) -> 交卷自动判分 -> 写入错题本
 */
@Service
public class PracticeService {

    private final PracticeRecordMapper recordMapper;
    private final PracticeAnswerMapper answerMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionService questionService;

    public PracticeService(PracticeRecordMapper recordMapper, PracticeAnswerMapper answerMapper,
                           QuestionMapper questionMapper, WrongQuestionMapper wrongQuestionMapper,
                           QuestionService questionService) {
        this.recordMapper = recordMapper;
        this.answerMapper = answerMapper;
        this.questionMapper = questionMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.questionService = questionService;
    }

    @Transactional
    public PracticeStartVO start(Long userId, PracticeStartDTO dto) {
        int count = Math.max(1, Math.min(50, dto.getCount()));
        int mode = dto.getMode();
        boolean random = mode == 2;
        boolean onlyWrong = dto.getOnlyWrong() || mode == 3;
        List<Question> questions = questionMapper.selectForPractice(
                userId, dto.getCategoryId(), dto.getBankId(), dto.getDifficulty(), dto.getType(), count, random, onlyWrong);
        if (questions.isEmpty()) {
            throw new BusinessException("没有符合条件的题目, 请调整筛选条件");
        }
        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setName(dto.getName() == null || dto.getName().isEmpty() ? defaultName(mode) : dto.getName());
        record.setMode(mode);
        record.setCategoryId(dto.getCategoryId());
        record.setTotal(questions.size());
        recordMapper.insert(record);

        PracticeStartVO vo = new PracticeStartVO();
        vo.setRecord(record);
        List<QuestionDTO> questionDTOs = new ArrayList<>();
        for (Question question : questions) {
            QuestionDTO item = questionService.toDTO(question);
            // 练习中不返回答案与解析, 防作弊
            item.setAnswer(null);
            item.setAnalysis(null);
            questionDTOs.add(item);
        }
        vo.setQuestions(questionDTOs);
        return vo;
    }

    @Transactional
    public Map<String, Object> submit(Long userId, PracticeSubmitDTO dto) {
        PracticeRecord record = recordMapper.findById(dto.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("练习记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new BusinessException("该练习已提交");
        }
        Map<Long, String> answerMap = new HashMap<>();
        if (dto.getAnswers() != null) {
            for (PracticeSubmitDTO.AnswerItem item : dto.getAnswers()) {
                if (item.getQuestionId() != null) {
                    answerMap.put(item.getQuestionId(), item.getAnswer());
                }
            }
        }
        List<Long> questionIds = new ArrayList<>(answerMap.keySet());
        List<PracticeAnswer> rows = new ArrayList<>();
        int correctCount = 0;
        if (!questionIds.isEmpty()) {
            List<Question> questions = questionMapper.selectByIds(questionIds);
            for (Question question : questions) {
                if (!question.getUserId().equals(userId)) {
                    continue;
                }
                String userAnswer = answerMap.get(question.getId());
                boolean correct = isCorrect(question, userAnswer);
                if (correct) {
                    correctCount++;
                }
                PracticeAnswer row = new PracticeAnswer();
                row.setRecordId(record.getId());
                row.setQuestionId(question.getId());
                row.setUserId(userId);
                row.setUserAnswer(userAnswer);
                row.setIsCorrect(correct ? 1 : 0);
                rows.add(row);
            }
        }
        if (!rows.isEmpty()) {
            answerMapper.insertBatch(rows);
        }
        int duration = (int) Duration.between(record.getStartTime() == null ? LocalDateTime.now() : record.getStartTime(),
                LocalDateTime.now()).getSeconds();
        record.setCorrect(correctCount);
        record.setDuration(Math.max(0, duration));
        record.setStatus(1);
        recordMapper.updateFinish(record);

        // 错题本维护: 答错入本; 错题重做答对则标记已掌握
        for (PracticeAnswer row : rows) {
            if (row.getIsCorrect() == 0) {
                WrongQuestion exist = wrongQuestionMapper.find(userId, row.getQuestionId());
                if (exist == null) {
                    WrongQuestion wrong = new WrongQuestion();
                    wrong.setUserId(userId);
                    wrong.setQuestionId(row.getQuestionId());
                    wrong.setLastAnswer(row.getUserAnswer());
                    wrongQuestionMapper.insert(wrong);
                } else {
                    wrongQuestionMapper.incrementWrong(exist.getId(), row.getUserAnswer());
                }
            } else if (record.getMode() != null && record.getMode() == 3) {
                wrongQuestionMapper.updateMastered(userId, row.getQuestionId(), 1);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("answers", answerMapper.selectByRecord(record.getId()));
        result.put("unanswered", record.getTotal() - rows.size());
        return result;
    }

    public PageInfo<PracticeRecord> records(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return new PageInfo<>(recordMapper.selectPage(userId));
    }

    public Map<String, Object> detail(Long userId, Long recordId) {
        PracticeRecord record = recordMapper.findById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("练习记录不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("answers", answerMapper.selectByRecord(recordId));
        return result;
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        PracticeRecord record = recordMapper.findById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("练习记录不存在");
        }
        answerMapper.deleteByRecord(recordId);
        recordMapper.deleteById(recordId);
    }

    /** 判分规则 */
    private boolean isCorrect(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }
        String answer = question.getAnswer() == null ? "" : question.getAnswer().trim();
        if (answer.isEmpty()) {
            return false;
        }
        String ua = userAnswer.trim();
        switch (question.getType() == null ? 0 : question.getType()) {
            case Constants.TYPE_MULTIPLE: {
                return normalizeLetters(ua).equals(normalizeLetters(answer));
            }
            case Constants.TYPE_FILL: {
                String[] expect = splitMultiBlank(answer);
                String[] actual = splitMultiBlank(ua);
                if (expect.length != actual.length) {
                    return false;
                }
                for (int i = 0; i < expect.length; i++) {
                    if (!expect[i].trim().equalsIgnoreCase(actual[i].trim())) {
                        return false;
                    }
                }
                return true;
            }
            case Constants.TYPE_JUDGE: {
                return normalizeJudge(ua).equals(normalizeJudge(answer));
            }
            default:
                // 单选/简答: 宽松相等比较
                return ua.equalsIgnoreCase(answer);
        }
    }

    /** 按字面量 '|||' 拆分多空答案(不使用正则) */
    private String[] splitMultiBlank(String s) {
        List<String> parts = new ArrayList<>();
        String sep = "|||";
        int start = 0;
        int idx;
        while ((idx = s.indexOf(sep, start)) >= 0) {
            parts.add(s.substring(start, idx));
            start = idx + sep.length();
        }
        parts.add(s.substring(start));
        return parts.toArray(new String[0]);
    }

    private String normalizeLetters(String s) {
        char[] chars = s.toUpperCase().replaceAll("[^A-Z]", "").toCharArray();
        java.util.Arrays.sort(chars);
        return new String(chars);
    }

    private String normalizeJudge(String s) {
        String v = s.trim();
        if (v.equals("对") || v.equals("正确") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("T")) {
            return "对";
        }
        return "错";
    }

    private String defaultName(int mode) {
        String modeName = mode == 2 ? "随机练习" : (mode == 3 ? "错题重做" : "顺序练习");
        return modeName + " " + LocalDateTime.now().toLocalDate();
    }
}
