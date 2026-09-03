package com.qbank.controller;

import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.dto.QuestionDTO;
import com.qbank.dto.QuestionQuery;
import com.qbank.entity.Category;
import com.qbank.entity.Question;
import com.qbank.entity.User;
import com.qbank.interceptor.LoginInterceptor;
import com.qbank.mapper.QuestionMapper;
import com.qbank.service.BankService;
import com.qbank.service.CategoryService;
import com.qbank.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目管理(服务端渲染版): 列表分页筛选 + 新增/编辑表单 + 删除
 */
@Controller
public class QuestionController {

    private final QuestionService questionService;
    private final CategoryService categoryService;
    private final BankService bankService;
    private final QuestionMapper questionMapper;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public QuestionController(QuestionService questionService, CategoryService categoryService,
                              BankService bankService, QuestionMapper questionMapper) {
        this.questionService = questionService;
        this.categoryService = categoryService;
        this.bankService = bankService;
        this.questionMapper = questionMapper;
    }

    // ==================== 列表: 筛选 + 分页 ====================

    @GetMapping("/questions")
    public String list(@ModelAttribute QuestionQuery query, Model model,
                       RedirectAttributes ra, HttpSession session) {
        User user = (User) session.getAttribute(LoginInterceptor.SESSION_USER);
        PageInfo<QuestionDTO> page;
        try {
            page = questionService.page(user.getId(), user.getRole(), query);
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/questions";
        }

        // 表头各列展示用数据(题目 DTO 不含 updateTime, 按本页 id 补查一次更新时间)
        Map<Long, String> updateTimeText = new HashMap<>();
        List<QuestionDTO> dtoList = page.getList();
        if (!dtoList.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (QuestionDTO d : dtoList) {
                ids.add(d.getId());
            }
            for (Question q : questionMapper.selectByIds(ids)) {
                updateTimeText.put(q.getId(),
                        q.getUpdateTime() == null ? "-" : TIME_FMT.format(q.getUpdateTime()));
            }
        }
        List<QuestionRow> rows = new ArrayList<>();
        for (QuestionDTO d : dtoList) {
            rows.add(new QuestionRow(d, updateTimeText.getOrDefault(d.getId(), "-")));
        }

        String baseQuery = buildBaseQuery(query);
        int pageNum = page.getPageNum();
        int pages = page.getPages();
        List<PageItem> pageItems = buildPageItems(baseQuery, pageNum, pages);

        addCommonModel(model, user);
        model.addAttribute("page", page);
        model.addAttribute("rows", rows);
        model.addAttribute("pageItems", pageItems);
        // 筛选回显(统一转字符串便于与下拉项 value 比较)
        model.addAttribute("selKeyword", query.getKeyword());
        model.addAttribute("selType", toStr(query.getType()));
        model.addAttribute("selDifficulty", toStr(query.getDifficulty()));
        model.addAttribute("selCategoryId", toStr(query.getCategoryId()));
        model.addAttribute("selBankId", toStr(query.getBankId()));
        return "questions";
    }

    // ==================== 新增/编辑表单页 ====================

    @GetMapping("/questions/form")
    public String form(@RequestParam(required = false) Long id, Model model,
                       RedirectAttributes ra, HttpSession session) {
        User user = (User) session.getAttribute(LoginInterceptor.SESSION_USER);
        QuestionForm form;
        if (id != null) {
            try {
                QuestionDTO dto = questionService.get(user.getId(), user.getRole(), id);
                form = QuestionForm.fromDTO(dto);
            } catch (BusinessException e) {
                ra.addFlashAttribute("flashError", e.getMessage());
                return "redirect:/questions";
            }
        } else {
            form = new QuestionForm();
        }
        addCommonModel(model, user);
        model.addAttribute("form", form);
        return "question_form";
    }

    // ==================== 保存(新增/更新) ====================

    @PostMapping("/questions/save")
    public String save(@ModelAttribute QuestionForm form, Model model,
                       RedirectAttributes ra, HttpSession session) {
        User user = (User) session.getAttribute(LoginInterceptor.SESSION_USER);
        boolean editing = form.getId() != null && StringUtils.hasText(form.getId());
        QuestionDTO dto = form.toDTO();
        try {
            if (editing) {
                questionService.update(user.getId(), user.getRole(), dto);
            } else {
                questionService.add(user.getId(), dto);
            }
            ra.addFlashAttribute("flashSuccess", editing ? "题目修改成功" : "题目新增成功");
            return "redirect:/questions";
        } catch (BusinessException e) {
            // 校验/权限失败: 回到表单页保留已填内容
            addCommonModel(model, user);
            model.addAttribute("form", form);
            model.addAttribute("error", e.getMessage());
            return "question_form";
        }
    }

    // ==================== 删除(单条) ====================

    @PostMapping("/questions/delete")
    public String delete(@RequestParam Long id, RedirectAttributes ra, HttpSession session) {
        User user = (User) session.getAttribute(LoginInterceptor.SESSION_USER);
        try {
            questionService.delete(user.getId(), user.getRole(), Collections.singletonList(id));
            ra.addFlashAttribute("flashSuccess", "删除成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/questions";
    }

    // ==================== 下拉数据 ====================

    private void addCommonModel(Model model, User user) {
        List<Option> typeOptions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            typeOptions.add(new Option(String.valueOf(i), Constants.TYPE_NAMES.get(i - 1)));
        }
        model.addAttribute("typeOptions", typeOptions);

        List<Option> difficultyOptions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            difficultyOptions.add(new Option(String.valueOf(i), Constants.DIFFICULTY_NAMES.get(i - 1)));
        }
        model.addAttribute("difficultyOptions", difficultyOptions);

        List<Category> tree = categoryService.tree(user.getId());
        model.addAttribute("categoryTree", tree);
        List<Option> categoryOptions = new ArrayList<>();
        for (Category root : tree) {
            categoryOptions.add(new Option(String.valueOf(root.getId()), root.getName()));
            if (root.getChildren() != null) {
                for (Category child : root.getChildren()) {
                    categoryOptions.add(new Option(String.valueOf(child.getId()),
                            root.getName() + " / " + child.getName()));
                }
            }
        }
        model.addAttribute("categoryOptions", categoryOptions);

        List<Option> bankOptions = new ArrayList<>();
        for (com.qbank.entity.Bank b : bankService.list(user.getId(), user.getRole())) {
            bankOptions.add(new Option(String.valueOf(b.getId()), b.getName()));
        }
        model.addAttribute("bankOptions", bankOptions);
    }

    // ==================== 分页链接 ====================

    private String buildBaseQuery(QuestionQuery query) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(query.getKeyword())) {
            parts.add("keyword=" + enc(query.getKeyword().trim()));
        }
        if (query.getCategoryId() != null) {
            parts.add("categoryId=" + query.getCategoryId());
        }
        if (query.getBankId() != null) {
            parts.add("bankId=" + query.getBankId());
        }
        if (query.getType() != null) {
            parts.add("type=" + query.getType());
        }
        if (query.getDifficulty() != null) {
            parts.add("difficulty=" + query.getDifficulty());
        }
        return String.join("&", parts);
    }

    private List<PageItem> buildPageItems(String baseQuery, int pageNum, int pages) {
        List<PageItem> items = new ArrayList<>();
        items.add(new PageItem("上一页", pageNum > 1 ? pageUrl(baseQuery, pageNum - 1) : null, false));
        if (pages > 0) {
            int from = Math.max(1, pageNum - 2);
            int to = Math.min(pages, pageNum + 2);
            if (from > 1) {
                items.add(new PageItem("1", pageUrl(baseQuery, 1), pageNum == 1));
                if (from > 2) {
                    items.add(new PageItem("…", null, false));
                }
            }
            for (int p = from; p <= to; p++) {
                items.add(new PageItem(String.valueOf(p), pageUrl(baseQuery, p), p == pageNum));
            }
            if (to < pages) {
                if (to < pages - 1) {
                    items.add(new PageItem("…", null, false));
                }
                items.add(new PageItem(String.valueOf(pages), pageUrl(baseQuery, pages), pageNum == pages));
            }
        }
        items.add(new PageItem("下一页", pageNum < pages ? pageUrl(baseQuery, pageNum + 1) : null, false));
        return items;
    }

    private String pageUrl(String baseQuery, int pageNum) {
        String url = "/questions?";
        if (!baseQuery.isEmpty()) {
            url += baseQuery + "&";
        }
        return url + "pageNum=" + pageNum;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String toStr(Object o) {
        return o == null ? null : o.toString();
    }

    // ==================== 视图模型 ====================

    /** 下拉选项: value 统一字符串, 便于与表单/回显值比较 */
    public static class Option {
        private final String value;
        private final String name;

        Option(String value, String name) {
            this.value = value;
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }

    /** 列表行展示 */
    public static class QuestionRow {
        private final Long id;
        private final String title;
        private final String typeName;
        private final String categoryName;
        private final String bankName;
        private final Integer difficulty;
        private final String difficultyName;
        private final String stars;
        private final String updateTime;

        QuestionRow(QuestionDTO d, String updateTime) {
            this.id = d.getId();
            this.title = d.getTitle();
            this.typeName = d.getType() == null ? "未知" : Constants.typeName(d.getType());
            this.categoryName = d.getCategoryName();
            this.bankName = d.getBankName();
            this.difficulty = d.getDifficulty();
            this.difficultyName = d.getDifficulty() == null ? "未知" : Constants.difficultyName(d.getDifficulty());
            this.stars = stars(d.getDifficulty());
            this.updateTime = updateTime;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public String getBankName() {
            return bankName;
        }

        public Integer getDifficulty() {
            return difficulty;
        }

        public String getDifficultyName() {
            return difficultyName;
        }

        public String getStars() {
            return stars;
        }

        public String getUpdateTime() {
            return updateTime;
        }

        private static String stars(Integer difficulty) {
            int d = difficulty == null ? 0 : Math.max(0, Math.min(5, difficulty));
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= 5; i++) {
                sb.append(i <= d ? "★" : "☆");
            }
            return sb.toString();
        }
    }

    /** 分页条一项 */
    public static class PageItem {
        private final String label;
        private final String url;      // null: 不可点(省略号或禁用)
        private final boolean current;

        PageItem(String label, String url, boolean current) {
            this.label = label;
            this.url = url;
            this.current = current;
        }

        public String getLabel() {
            return label;
        }

        public String getUrl() {
            return url;
        }

        public boolean isCurrent() {
            return current;
        }
    }

    /** 表单绑定模型: 全字符串字段, 空串在 controller 统一转 null */
    public static class QuestionForm {
        private String id;
        private String type = "1";
        private String difficulty = "3";
        private String categoryId;
        private String bankId;
        private String title;
        private String optionsTextarea;
        private String answer;
        private String analysis;
        private String tags;
        private String source;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public String getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(String categoryId) {
            this.categoryId = categoryId;
        }

        public String getBankId() {
            return bankId;
        }

        public void setBankId(String bankId) {
            this.bankId = bankId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getOptionsTextarea() {
            return optionsTextarea;
        }

        public void setOptionsTextarea(String optionsTextarea) {
            this.optionsTextarea = optionsTextarea;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public String getAnalysis() {
            return analysis;
        }

        public void setAnalysis(String analysis) {
            this.analysis = analysis;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        /** 编辑回显: DTO -> 表单(选项合并为多行文本; 判断题答案归一到 对/错) */
        static QuestionForm fromDTO(QuestionDTO dto) {
            QuestionForm f = new QuestionForm();
            f.id = dto.getId() == null ? null : String.valueOf(dto.getId());
            f.type = dto.getType() == null ? null : String.valueOf(dto.getType());
            f.difficulty = dto.getDifficulty() == null ? "3" : String.valueOf(dto.getDifficulty());
            f.categoryId = dto.getCategoryId() == null ? null : String.valueOf(dto.getCategoryId());
            f.bankId = dto.getBankId() == null ? null : String.valueOf(dto.getBankId());
            f.title = dto.getTitle();
            if (dto.getOptions() != null) {
                f.optionsTextarea = String.join("\n", dto.getOptions());
            }
            String answer = dto.getAnswer();
            if (dto.getType() != null && dto.getType() == Constants.TYPE_JUDGE) {
                f.answer = ("对".equals(answer) || "正确".equals(answer)) ? "对"
                        : ("错".equals(answer) || "错误".equals(answer)) ? "错" : "";
            } else {
                f.answer = answer;
            }
            f.analysis = dto.getAnalysis();
            f.tags = dto.getTags();
            f.source = dto.getSource();
            return f;
        }

        /** 表单 -> DTO: 空串 ID/分类/题库转 null; 选择题选项按行拆分 trim 去空行 */
        QuestionDTO toDTO() {
            QuestionDTO dto = new QuestionDTO();
            dto.setId(toLong(id));
            dto.setType(toInt(type));
            dto.setDifficulty(toInt(difficulty));
            dto.setCategoryId(toLong(categoryId));
            dto.setBankId(toLong(bankId));
            dto.setTitle(title == null ? null : title.trim());
            dto.setAnalysis(blankToNull(analysis));
            dto.setTags(blankToNull(tags));
            dto.setSource(blankToNull(source));
            Integer type = dto.getType();
            if (type != null && (type == Constants.TYPE_SINGLE || type == Constants.TYPE_MULTIPLE)) {
                List<String> options = new ArrayList<>();
                if (StringUtils.hasText(optionsTextarea)) {
                    for (String line : optionsTextarea.split("\\r?\\n")) {
                        String t = line.trim();
                        if (!t.isEmpty()) {
                            options.add(t);
                        }
                    }
                }
                dto.setOptions(options);
            }
            dto.setAnswer(normalizeAnswer(type, answer));
            return dto;
        }

        private static String normalizeAnswer(Integer type, String answer) {
            if (answer == null) {
                return null;
            }
            String ans = answer.trim();
            if (type != null && type == Constants.TYPE_MULTIPLE) {
                // 清理逗号/顿号/空格等分隔符并去重字母, 其余规范化交给 service.validate
                String cleaned = ans.toUpperCase().replaceAll("[\\s,，、;；]", "");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cleaned.length(); i++) {
                    char c = cleaned.charAt(i);
                    if (sb.indexOf(String.valueOf(c)) < 0) {
                        sb.append(c);
                    }
                }
                ans = sb.toString();
            }
            return ans;
        }

        private static Long toLong(String s) {
            if (!StringUtils.hasText(s)) {
                return null;
            }
            try {
                return Long.valueOf(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static Integer toInt(String s) {
            if (!StringUtils.hasText(s)) {
                return null;
            }
            try {
                return Integer.valueOf(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static String blankToNull(String s) {
            return StringUtils.hasText(s) ? s.trim() : null;
        }
    }
}
