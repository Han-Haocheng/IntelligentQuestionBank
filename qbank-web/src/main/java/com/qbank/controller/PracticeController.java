package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.common.Result;
import com.qbank.dto.NameValueVO;
import com.qbank.dto.PracticeStartDTO;
import com.qbank.dto.PracticeStartVO;
import com.qbank.dto.PracticeSubmitDTO;
import com.qbank.dto.QuestionDTO;
import com.qbank.entity.Bank;
import com.qbank.entity.Category;
import com.qbank.entity.PracticeAnswer;
import com.qbank.entity.PracticeRecord;
import com.qbank.entity.User;
import com.qbank.interceptor.LoginInterceptor;
import com.qbank.service.BankService;
import com.qbank.service.CategoryService;
import com.qbank.service.PracticeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 练习(服务端渲染版): 配置 -> 开始 -> 答题 -> 交卷 -> 成绩/记录
 */
@Controller
public class PracticeController {

    /** Session 中保存的"进行中练习"会话缓存: recordId -> 开始练习返回(含题目清单, 不含答案) */
    private static final String SESSION_PRACTICE_KEY = "qbankPracticeSessions";

    /** 选项字母标签(最多支持 10 个选项) */
    private static final String[] LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    private final PracticeService practiceService;
    private final CategoryService categoryService;
    private final BankService bankService;

    public PracticeController(PracticeService practiceService, CategoryService categoryService,
                              BankService bankService) {
        this.practiceService = practiceService;
        this.categoryService = categoryService;
        this.bankService = bankService;
    }

    /** 练习配置页 */
    @GetMapping("/practice")
    public String practicePage(Model model, HttpSession session) {
        User user = currentUser(session);
        populateConfig(model, user, new PracticeStartDTO());
        model.addAttribute("pageTitle", "开始练习");
        return "practice";
    }

    /** 开始练习: 生成练习记录 + 题目快照, 进入答题页 */
    @PostMapping("/practice/start")
    public String start(@ModelAttribute("practiceForm") PracticeStartDTO dto, Model model,
                        HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        try {
            if (dto.getMode() == null || dto.getMode() < Constants.PRACTICE_MODE_SEQUENCE
                    || dto.getMode() > Constants.PRACTICE_MODE_WRONG) {
                throw new BusinessException("请选择练习模式");
            }
            if (dto.getMode() == Constants.PRACTICE_MODE_WRONG) {
                // 错题重做模式强制只取错题本中的题目
                dto.setOnlyWrong(true);
            }
            PracticeStartVO vo = practiceService.start(user.getId(), dto);
            practiceSessions(session).put(vo.getRecord().getId(), vo);
            return "redirect:/practice/do?recordId=" + vo.getRecord().getId();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            populateConfig(model, user, dto);
            model.addAttribute("pageTitle", "开始练习");
            return "practice";
        }
    }

    /** 答题页(展示题干与选项, 不显示答案/解析) */
    @GetMapping("/practice/do")
    public String doPage(@RequestParam Long recordId, Model model, HttpSession session,
                         RedirectAttributes ra) {
        User user = currentUser(session);
        PracticeStartVO vo = practiceSessions(session).get(recordId);
        if (vo == null || vo.getRecord() == null) {
            ra.addFlashAttribute("flashError", "练习会话不存在或已过期, 请重新开始练习");
            return "redirect:/practice";
        }
        // 已交卷过的记录直接跳到成绩页
        Map<String, Object> detail = practiceService.detail(user.getId(), recordId);
        PracticeRecord record = (PracticeRecord) detail.get("record");
        if (record.getStatus() != null && record.getStatus() == Constants.PRACTICE_STATUS_FINISHED) {
            return "redirect:/practice/result?recordId=" + recordId;
        }
        List<QuestionDTO> questions = vo.getQuestions() == null ? Collections.emptyList() : vo.getQuestions();
        model.addAttribute("record", vo.getRecord());
        model.addAttribute("questions", buildDoItems(questions));
        model.addAttribute("total", questions.size());
        model.addAttribute("recordId", recordId);
        model.addAttribute("modeName", modeName(record.getMode()));
        populateNameMaps(model);
        model.addAttribute("pageTitle", "答题中");
        return "practice_do";
    }

    /** 交卷: 收集 answer_<questionId> 表单答案后由服务层判分并维护错题本 */
    @PostMapping("/practice/submit")
    public String submit(@RequestParam Long recordId, HttpServletRequest request, HttpSession session,
                         RedirectAttributes ra) {
        User user = currentUser(session);
        PracticeSubmitDTO dto = new PracticeSubmitDTO();
        dto.setRecordId(recordId);
        dto.setAnswers(collectAnswers(request));
        try {
            practiceService.submit(user.getId(), dto);
        } catch (BusinessException e) {
            // 如"该练习已提交": 仍回到成绩页, 用 flash 提示原因
            ra.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/practice/result?recordId=" + recordId;
        }
        practiceSessions(session).remove(recordId);
        return "redirect:/practice/result?recordId=" + recordId;
    }

    /** 成绩/结果页(交卷后展示对错、正确答案与解析) */
    @GetMapping("/practice/result")
    public String result(@RequestParam Long recordId, Model model, HttpSession session) {
        User user = currentUser(session);
        Map<String, Object> detail = practiceService.detail(user.getId(), recordId);
        PracticeRecord record = (PracticeRecord) detail.get("record");
        List<PracticeAnswer> answers = answerList(detail);
        int total = record.getTotal() == null ? 0 : record.getTotal();
        model.addAttribute("record", record);
        model.addAttribute("answers", answers);
        model.addAttribute("answered", answers.size());
        model.addAttribute("unanswered", Math.max(0, total - answers.size()));
        model.addAttribute("accuracy", accuracy(record));
        model.addAttribute("modeName", modeName(record.getMode()));
        populateNameMaps(model);
        model.addAttribute("pageTitle", "练习成绩");
        return "practice_result";
    }

    /** 练习记录列表(分页) */
    @GetMapping("/practice/records")
    public String records(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          Model model, HttpSession session) {
        User user = currentUser(session);
        PageInfo<PracticeRecord> pageInfo = practiceService.records(user.getId(), pageNum, pageSize);
        model.addAttribute("pageInfo", pageInfo);
        populateNameMaps(model);
        model.addAttribute("pageTitle", "练习记录");
        return "practice_records";
    }

    /** 练习记录明细(结构同后端 /api/practice/records/{id}: record + answers) */
    @GetMapping("/practice/records/{id}")
    public String recordDetail(@PathVariable Long id, Model model, HttpSession session) {
        User user = currentUser(session);
        Map<String, Object> detail = practiceService.detail(user.getId(), id);
        List<PracticeAnswer> answers = answerList(detail);
        PracticeRecord record = (PracticeRecord) detail.get("record");
        int total = record.getTotal() == null ? 0 : record.getTotal();
        model.addAttribute("record", record);
        model.addAttribute("answers", answers);
        model.addAttribute("answered", answers.size());
        model.addAttribute("unanswered", Math.max(0, total - answers.size()));
        model.addAttribute("accuracy", accuracy(record));
        model.addAttribute("modeName", modeName(record.getMode()));
        populateNameMaps(model);
        model.addAttribute("fromDetail", true);
        model.addAttribute("pageTitle", "练习记录明细");
        return "practice_result";
    }

    /** 配置页联动: 当前筛选条件下可练习的题数(fetch 异步调用) */
    @GetMapping("/practice/count")
    @ResponseBody
    public Result<Integer> count(@RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) Long bankId,
                                 @RequestParam(required = false) Integer difficulty,
                                 @RequestParam(required = false) Integer type,
                                 @RequestParam(defaultValue = "false") boolean onlyWrong,
                                 HttpSession session) {
        User user = currentUser(session);
        try {
            return Result.ok(practiceService.count(user.getId(), categoryId, bankId, difficulty, type, onlyWrong));
        } catch (BusinessException e) {
            return Result.error(e.getMessage());
        }
    }

    // ---------- 私有辅助 ----------

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute(LoginInterceptor.SESSION_USER);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, PracticeStartVO> practiceSessions(HttpSession session) {
        Object cached = session.getAttribute(SESSION_PRACTICE_KEY);
        if (cached instanceof Map) {
            return (Map<Long, PracticeStartVO>) cached;
        }
        Map<Long, PracticeStartVO> map = new HashMap<>();
        session.setAttribute(SESSION_PRACTICE_KEY, map);
        return map;
    }

    /** 配置页下拉与常量 */
    private void populateConfig(Model model, User user, PracticeStartDTO dto) {
        List<NameValueVO> categoryOptions = new ArrayList<>();
        for (Category root : categoryService.tree(user.getId())) {
            categoryOptions.add(new NameValueVO(root.getName(), root.getId()));
            if (root.getChildren() != null) {
                for (Category child : root.getChildren()) {
                    categoryOptions.add(new NameValueVO("　└ " + child.getName(), child.getId()));
                }
            }
        }
        List<NameValueVO> bankOptions = new ArrayList<>();
        for (Bank bank : bankService.list(user.getId(), user.getRole())) {
            String label = bank.getName();
            if (bank.getQuestionCount() != null) {
                label += "（" + bank.getQuestionCount() + " 题）";
            }
            bankOptions.add(new NameValueVO(label, bank.getId()));
        }
        model.addAttribute("practiceForm", dto);
        model.addAttribute("categoryOptions", categoryOptions);
        model.addAttribute("bankOptions", bankOptions);
        model.addAttribute("typeNames", Constants.TYPE_NAMES);
        model.addAttribute("difficultyNames", Constants.DIFFICULTY_NAMES);
        populateNameMaps(model);
    }

    /** 页面通用名称映射(题型/难度/模式徽标等) */
    private void populateNameMaps(Model model) {
        Map<Integer, String> typeMap = new HashMap<>();
        Map<Integer, String> difficultyMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            typeMap.put(i, Constants.typeName(i));
            difficultyMap.put(i, Constants.difficultyName(i));
        }
        Map<Integer, String> modeMap = new HashMap<>();
        modeMap.put(Constants.PRACTICE_MODE_SEQUENCE, "顺序练习");
        modeMap.put(Constants.PRACTICE_MODE_RANDOM, "随机练习");
        modeMap.put(Constants.PRACTICE_MODE_WRONG, "错题重做");
        model.addAttribute("typeNameMap", typeMap);
        model.addAttribute("difficultyNameMap", difficultyMap);
        model.addAttribute("modeNameMap", modeMap);
    }

    /** 把会话中的题目转成答题页视图数据(每题带 A/B/C.. 选项标签) */
    private List<Map<String, Object>> buildDoItems(List<QuestionDTO> questions) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (QuestionDTO q : questions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("type", q.getType());
            item.put("title", q.getTitle());
            item.put("difficulty", q.getDifficulty());
            List<Map<String, String>> options = new ArrayList<>();
            if (q.getOptions() != null) {
                for (int i = 0; i < q.getOptions().size() && i < LETTERS.length; i++) {
                    Map<String, String> opt = new LinkedHashMap<>();
                    opt.put("letter", LETTERS[i]);
                    opt.put("text", q.getOptions().get(i) == null ? "" : q.getOptions().get(i));
                    options.add(opt);
                }
            }
            item.put("options", options);
            result.add(item);
        }
        return result;
    }

    /** 从表单参数 answer_<questionId> 收集答案(多选多值直接拼接, 判分侧会做字母归一化) */
    private List<PracticeSubmitDTO.AnswerItem> collectAnswers(HttpServletRequest request) {
        List<PracticeSubmitDTO.AnswerItem> answers = new ArrayList<>();
        String prefix = "answer_";
        Map<String, String[]> params = request.getParameterMap();
        if (params == null) {
            return answers;
        }
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith(prefix)) {
                continue;
            }
            Long questionId;
            try {
                questionId = Long.valueOf(key.substring(prefix.length()));
            } catch (NumberFormatException e) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    if (value != null) {
                        String trimmed = value.trim();
                        if (!trimmed.isEmpty()) {
                            sb.append(trimmed);
                        }
                    }
                }
            }
            if (sb.length() == 0) {
                continue;
            }
            PracticeSubmitDTO.AnswerItem item = new PracticeSubmitDTO.AnswerItem();
            item.setQuestionId(questionId);
            item.setAnswer(sb.toString());
            answers.add(item);
        }
        return answers;
    }

    @SuppressWarnings("unchecked")
    private List<PracticeAnswer> answerList(Map<String, Object> detail) {
        Object answers = detail.get("answers");
        return answers == null ? Collections.emptyList() : (List<PracticeAnswer>) answers;
    }

    /** 正确率(百分比, 保留1位小数) */
    private double accuracy(PracticeRecord record) {
        int total = record.getTotal() == null ? 0 : record.getTotal();
        int correct = record.getCorrect() == null ? 0 : record.getCorrect();
        if (total == 0) {
            return 0.0;
        }
        return Math.round(correct * 1000.0 / total) / 10.0;
    }

    private String modeName(Integer mode) {
        if (mode == null) {
            return "";
        }
        return mode == Constants.PRACTICE_MODE_RANDOM ? "随机练习"
                : (mode == Constants.PRACTICE_MODE_WRONG ? "错题重做" : "顺序练习");
    }
}
