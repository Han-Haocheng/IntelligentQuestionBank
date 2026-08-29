package com.qbank.entity;

import java.time.LocalDateTime;

/**
 * 题目
 */
public class Question {

    private Long id;
    private Long userId;
    private Long categoryId;
    private Long bankId;         // 所属题库
    private Integer type;        // 1单选 2多选 3填空 4判断 5简答
    private String title;
    private String options;      // JSON数组字符串
    private String answer;
    private String analysis;
    private Integer difficulty;  // 1~5
    private String tags;
    private String source;
    private Long originQuestionId;  // 拷贝来源题目ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 非数据库字段 */
    private String categoryName;
    private String bankName;
    private Boolean favorited;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
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
    public Long getOriginQuestionId() { return originQuestionId; }
    public void setOriginQuestionId(Long originQuestionId) { this.originQuestionId = originQuestionId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
}
