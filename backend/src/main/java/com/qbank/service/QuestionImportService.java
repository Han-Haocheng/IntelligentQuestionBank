package com.qbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qbank.common.BusinessException;
import com.qbank.dto.ImportRowDTO;
import com.qbank.entity.Bank;
import com.qbank.entity.Category;
import com.qbank.entity.Question;
import com.qbank.mapper.BankMapper;
import com.qbank.mapper.CategoryMapper;
import com.qbank.mapper.QuestionMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入服务: 支持 Excel(.xlsx) 与 CSV(UTF-8/GBK 自动识别)
 * 固定列: 题干 | 题型 | 选项A-F | 答案 | 解析 | 难度 | 知识点标签 | 来源
 */
@Service
public class QuestionImportService {

    private static final String[] HEADERS = {
            "题干", "题型", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F",
            "答案", "解析", "难度", "知识点标签", "来源"
    };

    private final QuestionMapper questionMapper;
    private final BankMapper bankMapper;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    public QuestionImportService(QuestionMapper questionMapper, BankMapper bankMapper,
                                 CategoryMapper categoryMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.bankMapper = bankMapper;
        this.categoryMapper = categoryMapper;
        this.objectMapper = objectMapper;
    }

    // ==================== 解析(预览, 不落库) ====================

    public List<ImportRowDTO> parse(String filename, InputStream in) {
        if (filename == null) {
            throw new BusinessException("无法识别文件类型");
        }
        String lower = filename.toLowerCase();
        try {
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return parseExcel(in);
            }
            if (lower.endsWith(".csv")) {
                return parseCsv(in.readAllBytes());
            }
            throw new BusinessException("仅支持 .xlsx / .csv 文件");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("文件解析失败: " + e.getMessage());
        }
    }

    private List<ImportRowDTO> parseExcel(InputStream in) throws Exception {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            requireHeader(header == null ? null : readRow(header));
            List<ImportRowDTO> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) {
                    continue;
                }
                List<String> cells = readRow(r);
                if (isEmptyRow(cells)) {
                    continue;
                }
                rows.add(toDTO(i + 1, cells));
            }
            return rows;
        }
    }

    private List<ImportRowDTO> parseCsv(byte[] raw) {
        String text = new String(decode(raw), StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<List<String>> table = splitCsv(text);
        if (table.isEmpty()) {
            throw new BusinessException("CSV 文件为空");
        }
        requireHeader(table.get(0));
        List<ImportRowDTO> rows = new ArrayList<>();
        for (int i = 1; i < table.size(); i++) {
            List<String> cells = table.get(i);
            if (isEmptyRow(cells)) {
                continue;
            }
            rows.add(toDTO(i + 1, cells));
        }
        return rows;
    }

    /** UTF-8 严格解码, 失败时回退 GBK(Excel 导出的中文 CSV 常见) */
    private byte[] decode(byte[] raw) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(java.nio.ByteBuffer.wrap(raw));
            return raw;
        } catch (CharacterCodingException e) {
            return new String(raw, java.nio.charset.Charset.forName("GBK"))
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    /** CSV 状态机解析, 支持引号包裹与转义双引号 */
    private List<List<String>> splitCsv(String text) {
        List<List<String>> table = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuote) {
                if (c == 34) {
                    if (i + 1 < text.length() && text.charAt(i + 1) == 34) {
                        cell.append(34);
                        i++;
                    } else {
                        inQuote = false;
                    }
                } else {
                    cell.append(c);
                }
            } else if (c == 34) {
                inQuote = true;
            } else if (c == 44) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == 13) {
                // 忽略
            } else if (c == 10) {
                row.add(cell.toString());
                cell.setLength(0);
                table.add(row);
                row = new ArrayList<>();
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            table.add(row);
        }
        return table;
    }

    private void requireHeader(List<String> header) {
        if (header == null || header.size() < 2
                || !"题干".equals(header.get(0).trim()) || !"题型".equals(header.get(1).trim())) {
            throw new BusinessException("表头不符合模板: 第一列须为「题干」, 第二列须为「题型」, 请下载模板后填写");
        }
    }

    private List<String> readRow(Row r) {
        int last = Math.max(r.getLastCellNum(), HEADERS.length);
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < last; i++) {
            cells.add(cellText(r.getCell(i)));
        }
        return cells;
    }

    private String cellText(Cell c) {
        if (c == null) {
            return "";
        }
        if (c.getCellType() == CellType.NUMERIC) {
            double v = c.getNumericCellValue();
            if (v == Math.floor(v)) {
                return String.valueOf((long) v);
            }
            return String.valueOf(v);
        }
        if (c.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(c.getBooleanCellValue());
        }
        String s = c.toString();
        return s == null ? "" : s.trim();
    }

    private boolean isEmptyRow(List<String> cells) {
        return cells.stream().allMatch(s -> s == null || s.trim().isEmpty());
    }

    // ==================== 行 DTO 与校验 ====================

    private ImportRowDTO toDTO(int rowNo, List<String> cells) {
        ImportRowDTO dto = new ImportRowDTO();
        dto.setRowNo(rowNo);
        dto.setTitle(at(cells, 0));
        dto.setTypeName(at(cells, 1));
        List<String> options = new ArrayList<>();
        for (int i = 2; i <= 7; i++) {
            String o = at(cells, i);
            if (!o.isEmpty()) {
                options.add(o);
            }
        }
        dto.setOptions(options);
        dto.setAnswer(at(cells, 8));
        dto.setAnalysis(at(cells, 9));
        String diff = at(cells, 10);
        dto.setDifficulty(diff.isEmpty() ? null : Integer.valueOf(safeInt(diff)));
        dto.setTags(at(cells, 11));
        dto.setSource(at(cells, 12));
        validate(dto);
        return dto;
    }

    private String at(List<String> cells, int i) {
        return i < cells.size() && cells.get(i) != null ? cells.get(i).trim() : "";
    }

    private String safeInt(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 48 && c <= 57) {
                sb.append(c);
            }
        }
        return sb.length() > 0 ? sb.toString() : "0";
    }

    /** 校验并规整单行; 错误写入 row.errors */
    public void validate(ImportRowDTO dto) {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.hasText(dto.getTitle())) {
            errors.add("题干不能为空");
        }
        Integer type = resolveType(dto.getTypeName());
        dto.setType(type);
        if (type == null) {
            errors.add("题型无法识别(须为 单选题/多选题/填空题/判断题/简答题)");
        }
        if (type != null) {
            normalize(type, dto, errors);
        }
        Integer diff = dto.getDifficulty();
        if (diff == null) {
            dto.setDifficulty(3);
        } else if (diff < 1 || diff > 5) {
            errors.add("难度须为 1-5");
        }
        dto.setErrors(errors);
    }

    private Integer resolveType(String name) {
        if (name == null) {
            return null;
        }
        String s = name.trim();
        if (s.startsWith("单选") || "1".equals(s)) { return 1; }
        if (s.startsWith("多选") || "2".equals(s)) { return 2; }
        if (s.startsWith("填空") || "3".equals(s)) { return 3; }
        if (s.startsWith("判断") || "4".equals(s)) { return 4; }
        if (s.startsWith("简答") || "5".equals(s)) { return 5; }
        return null;
    }

    private void normalize(int type, ImportRowDTO dto, List<String> errors) {
        String answer = dto.getAnswer() == null ? "" : dto.getAnswer().trim();
        if (type == 1 || type == 2) {
            if (dto.getOptions() == null || dto.getOptions().size() < 2) {
                errors.add("选择题至少需要 2 个选项");
            }
            if (answer.isEmpty()) {
                errors.add("选择题答案不能为空");
            } else {
                String letters = answer.replaceAll("[^A-Za-z]", "").toUpperCase();
                int max = dto.getOptions() == null ? 0 : dto.getOptions().size();
                if (letters.isEmpty()) {
                    errors.add("答案须为选项字母(如 A 或 ABD)");
                } else if (type == 1 && letters.length() != 1) {
                    errors.add("单选题答案只能有一个字母");
                } else {
                    for (char c : letters.toCharArray()) {
                        if (c - 65 >= max) {
                            errors.add("答案 " + c + " 超出选项范围");
                            break;
                        }
                    }
                }
                dto.setAnswer(sortLetters(letters));
            }
        } else if (type == 3) {
            if (answer.isEmpty()) {
                errors.add("填空题答案不能为空(多空用 ||| 分隔)");
            }
        } else if (type == 4) {
            String lower = answer.toLowerCase();
            if (answer.isEmpty()) {
                errors.add("判断题答案不能为空");
            } else if ("对".equals(answer) || "正确".equals(answer) || "是".equals(answer)
                    || "true".equals(lower) || "t".equals(lower) || answer.contains("√")) {
                dto.setAnswer("对");
            } else if ("错".equals(answer) || "错误".equals(answer) || "否".equals(answer)
                    || "false".equals(lower) || "f".equals(lower) || answer.contains("×") || answer.contains("x")) {
                dto.setAnswer("错");
            } else {
                errors.add("判断题答案须为 对/错");
            }
        } else if (type == 5 && answer.isEmpty()) {
            errors.add("简答题参考答案不能为空");
        }
    }

    private String sortLetters(String letters) {
        char[] cs = letters.toCharArray();
        java.util.Arrays.sort(cs);
        return new String(cs);
    }

    // ==================== 保存 ====================

    public java.util.Map<String, Object> save(Long userId, List<ImportRowDTO> rows,
                                              Long categoryId, Long bankId) {
        checkRefs(userId, categoryId, bankId);
        int success = 0;
        List<java.util.Map<String, Object>> failures = new ArrayList<>();
        for (ImportRowDTO dto : rows) {
            validate(dto);
            if (dto.getErrors() != null && !dto.getErrors().isEmpty()) {
                java.util.Map<String, Object> fail = new java.util.HashMap<>();
                fail.put("rowNo", dto.getRowNo());
                fail.put("reason", String.join("; ", dto.getErrors()));
                failures.add(fail);
                continue;
            }
            questionMapper.insert(toQuestion(userId, dto, categoryId, bankId));
            success++;
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("successCount", success);
        result.put("failures", failures);
        return result;
    }

    private void checkRefs(Long userId, Long categoryId, Long bankId) {
        if (categoryId != null) {
            Category c = categoryMapper.findById(categoryId);
            if (c == null || !c.getUserId().equals(userId)) {
                throw new BusinessException("所选分类不存在或无权使用");
            }
        }
        if (bankId != null) {
            Bank b = bankMapper.findById(bankId);
            if (b == null || !b.getUserId().equals(userId)) {
                throw new BusinessException("所选题库不存在或无权使用");
            }
        }
    }

    private Question toQuestion(Long userId, ImportRowDTO dto, Long categoryId, Long bankId) {
        Question q = new Question();
        q.setUserId(userId);
        q.setCategoryId(categoryId);
        q.setBankId(bankId);
        q.setType(dto.getType());
        q.setTitle(dto.getTitle());
        if ((dto.getType() == 1 || dto.getType() == 2) && dto.getOptions() != null) {
            try {
                q.setOptions(objectMapper.writeValueAsString(dto.getOptions()));
            } catch (Exception e) {
                throw new BusinessException("选项序列化失败");
            }
        }
        q.setAnswer(dto.getAnswer());
        q.setAnalysis(dto.getAnalysis());
        q.setDifficulty(dto.getDifficulty());
        q.setTags(dto.getTags());
        q.setSource(dto.getSource());
        return q;
    }

    // ==================== 模板下载 ====================

    public byte[] template() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("题目导入模板");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                sheet.setColumnWidth(i, i == 0 ? 9000 : 3600);
            }
            Row ex1 = sheet.createRow(1);
            ex1.createCell(0).setCellValue("Java 中，下列哪个关键字用于定义类？");
            ex1.createCell(1).setCellValue("单选题");
            ex1.createCell(2).setCellValue("class");
            ex1.createCell(3).setCellValue("interface");
            ex1.createCell(4).setCellValue("struct");
            ex1.createCell(5).setCellValue("package");
            ex1.createCell(8).setCellValue("A");
            ex1.createCell(9).setCellValue("class 用于定义类。");
            ex1.createCell(10).setCellValue(1);
            ex1.createCell(11).setCellValue("Java基础,关键字");
            ex1.createCell(12).setCellValue("自编");
            Row ex2 = sheet.createRow(2);
            ex2.createCell(0).setCellValue("Java 是一种面向对象的编程语言。");
            ex2.createCell(1).setCellValue("判断题");
            ex2.createCell(8).setCellValue("对");
            ex2.createCell(9).setCellValue("Java 以类和对象为核心。");
            ex2.createCell(10).setCellValue(1);
            ex2.createCell(11).setCellValue("Java概述");
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("模板生成失败: " + e.getMessage());
        }
    }

}