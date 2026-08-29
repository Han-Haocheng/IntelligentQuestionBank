package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.dto.AiResultVO;
import com.qbank.dto.NameValueVO;
import com.qbank.dto.OverviewVO;
import com.qbank.dto.QuestionDTO;
import com.qbank.dto.TrendVO;
import com.qbank.entity.AiAnalysis;
import com.qbank.entity.Question;
import com.qbank.mapper.AiAnalysisMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.util.AiHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 分析服务: 配置 API Key 时调用大模型, 否则降级为本地规则分析
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiHttpClient aiClient;
    private final AiAnalysisMapper aiAnalysisMapper;
    private final QuestionMapper questionMapper;
    private final QuestionService questionService;
    private final StatsService statsService;

    public AiService(AiHttpClient aiClient, AiAnalysisMapper aiAnalysisMapper,
                     QuestionMapper questionMapper, QuestionService questionService,
                     StatsService statsService) {
        this.aiClient = aiClient;
        this.aiAnalysisMapper = aiAnalysisMapper;
        this.questionMapper = questionMapper;
        this.questionService = questionService;
        this.statsService = statsService;
    }

    public AiResultVO analyzeQuestion(Long userId, Long questionId) {
        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        QuestionDTO dto = questionService.toDTO(question);
        String prompt = buildQuestionPrompt(dto);
        String content = callAi(prompt, () -> localQuestionAnalysis(dto));
        saveAnalysis(userId, questionId, 1, content);
        return new AiResultVO(content, currentModel());
    }

    public AiResultVO report(Long userId) {
        OverviewVO overview = statsService.overview(userId);
        List<NameValueVO> wrongByCategory = statsService.wrongByCategory(userId);
        List<TrendVO> trend = statsService.trend(userId);
        String prompt = buildReportPrompt(overview, wrongByCategory, trend);
        String content = callAi(prompt, () -> localReport(overview, wrongByCategory));
        saveAnalysis(userId, null, 2, content);
        return new AiResultVO(content, currentModel());
    }

    public PageInfo<AiAnalysis> history(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return new PageInfo<>(aiAnalysisMapper.selectPage(userId));
    }

    private String callAi(String prompt, java.util.function.Supplier<String> fallback) {
        if (aiClient.isEnabled()) {
            try {
                return aiClient.chat(prompt);
            } catch (Exception e) {
                log.warn("AI 调用失败, 降级为本地分析: {}", e.getMessage());
                return fallback.get();
            }
        }
        return fallback.get();
    }

    private String currentModel() {
        return aiClient.isEnabled() ? "AI" : "local-rules";
    }

    private void saveAnalysis(Long userId, Long questionId, int type, String content) {
        AiAnalysis analysis = new AiAnalysis();
        analysis.setUserId(userId);
        analysis.setQuestionId(questionId);
        analysis.setType(type);
        analysis.setContent(content);
        analysis.setModel(currentModel());
        aiAnalysisMapper.insert(analysis);
    }

    private String buildQuestionPrompt(QuestionDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名资深教师。请对下面这道题目进行分析，用中文分点输出：\n")
          .append("1. 考查知识点；2. 难度评估(1-5)及理由；3. 题目表述清晰度与改进建议；4. 解题思路提示(不要直接给出完整答案)。\n\n")
          .append("题型: ").append(Constants.typeName(dto.getType() == null ? 1 : dto.getType())).append("\n")
          .append("难度: ").append(dto.getDifficulty()).append("\n")
          .append("知识点标签: ").append(dto.getTags() == null ? "无" : dto.getTags()).append("\n")
          .append("题干: ").append(dto.getTitle()).append("\n");
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            sb.append("选项:\n");
            for (int i = 0; i < dto.getOptions().size(); i++) {
                sb.append((char) ('A' + i)).append(". ").append(dto.getOptions().get(i)).append("\n");
            }
        }
        sb.append("参考答案: ").append(dto.getAnswer() == null ? "无" : dto.getAnswer()).append("\n")
          .append("解析: ").append(dto.getAnalysis() == null ? "无" : dto.getAnalysis());
        return sb.toString();
    }

    private String buildReportPrompt(OverviewVO overview, List<NameValueVO> wrongByCategory, List<TrendVO> trend) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名学习顾问。请根据以下练习数据，用中文分点输出学情报告：\n")
          .append("1. 总体表现评价；2. 薄弱知识点分析；3. 具体学习建议；4. 后续练习计划建议。\n\n")
          .append("题库题目数: ").append(overview.getQuestionCount()).append("\n")
          .append("练习次数: ").append(overview.getPracticeCount()).append("\n")
          .append("总正确率: ").append(overview.getAccuracy()).append("%\n")
          .append("错题本未掌握: ").append(overview.getWrongCount()).append(" 题\n")
          .append("易错分类Top5: ").append(formatNameValues(wrongByCategory)).append("\n")
          .append("近14天练习(日期=题数/正确率%): ");
        for (TrendVO item : trend) {
            if (item.getTotal() != null && item.getTotal() > 0) {
                sb.append(item.getDate()).append("=").append(item.getTotal())
                  .append("题/").append(item.getAccuracy()).append("%; ");
            }
        }
        return sb.toString();
    }

    private String formatNameValues(List<NameValueVO> list) {
        if (list == null || list.isEmpty()) {
            return "暂无";
        }
        StringBuilder sb = new StringBuilder();
        for (NameValueVO item : list) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item.getName()).append("(").append(item.getValue()).append("题)");
        }
        return sb.toString();
    }

    // ==================== 本地规则分析(未配置AI Key时降级) ====================

    private String localQuestionAnalysis(QuestionDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("【本地规则分析】(未配置 AI Key, 在 application.yml 的 qbank.ai.api-key 中配置后可启用大模型分析)\n\n");
        sb.append("1. 考查知识点: ").append(dto.getTags() == null || dto.getTags().isEmpty()
                ? "未标注, 建议补充知识点标签" : dto.getTags()).append("\n");
        int diff = dto.getDifficulty() == null ? 3 : dto.getDifficulty();
        int titleLen = dto.getTitle() == null ? 0 : dto.getTitle().length();
        sb.append("2. 难度评估: 当前标注为 ").append(Constants.difficultyName(diff))
          .append("(").append(diff).append("/5)");
        if (titleLen > 200) {
            sb.append(", 题干较长, 实际难度可能高于标注, 建议复核");
        } else if (titleLen < 20) {
            sb.append(", 题干简短, 可能偏基础题");
        }
        sb.append("\n3. 题目表述建议:\n");
        sb.append("   - ").append(titleLen < 15 ? "题干偏短, 建议补充题设条件, 避免歧义" : "题干长度适中, 表述基本完整").append("\n");
        if (dto.getType() != null && (dto.getType() == 1 || dto.getType() == 2)) {
            int opts = dto.getOptions() == null ? 0 : dto.getOptions().size();
            sb.append("   - 选项数量为 ").append(opts).append(opts >= 4 ? ", 符合常规命题习惯" : ", 建议提供4个及以上选项") .append("\n");
            sb.append("   - 检查选项间是否互相独立、避免包含关系\n");
        }
        sb.append("   - ").append(dto.getAnalysis() == null || dto.getAnalysis().isEmpty()
                ? "尚未填写解析, 建议补充解析便于练习后复盘" : "已含解析, 可补充常见错误选项分析").append("\n");
        sb.append("4. 解题思路提示: 请结合知识点先判断题目类型, 再回忆对应公式/定义, 最后代入验证选项或答案。");
        return sb.toString();
    }

    private String localReport(OverviewVO overview, List<NameValueVO> wrongByCategory) {
        StringBuilder sb = new StringBuilder();
        sb.append("【本地规则分析】(未配置 AI Key, 在 application.yml 的 qbank.ai.api-key 中配置后可启用大模型报告)\n\n");
        sb.append("1. 总体表现: 共练习 ").append(overview.getPracticeCount()).append(" 次, 总正确率 ")
          .append(overview.getAccuracy()).append("%");
        double acc = overview.getAccuracy() == null ? 0 : overview.getAccuracy();
        if (acc >= 85) {
            sb.append(", 掌握情况优秀, 可挑战更高难度题目。");
        } else if (acc >= 70) {
            sb.append(", 掌握情况良好, 注意错题复盘。");
        } else {
            sb.append(", 正确率偏低, 建议从基础题重新巩固。");
        }
        sb.append("\n2. 薄弱知识点: ").append(formatNameValues(wrongByCategory))
          .append("\n3. 学习建议: 优先复习易错分类对应的知识点; 每天保持一次小规模练习(10题左右)。\n");
        sb.append("4. 练习计划: 错题本现有 ").append(overview.getWrongCount())
          .append(" 题未掌握, 建议使用『错题重做』模式循环巩固, 全部掌握后再扩充新题。");
        return sb.toString();
    }
}
