package com.qbank.dto;

import java.util.List;

/**
 * 批量导入单行数据(解析预览 + 保存共用)
 */
public class ImportRowDTO {

    /** 源文件行号(从1开始, 含表头) */
    private Integer rowNo;
    private String title;
    /** 题型名称或编号: 单选题/多选题/填空题/判断题/简答题 或 1-5 */
    private String typeName;
    private Integer type;
    /** 选项, 按需 2-6 个 */
    private List<String> options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private String tags;
    private String source;
    /** 解析阶段的校验错误(为空表示该行可导入) */
    private List<String> errors;

    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
