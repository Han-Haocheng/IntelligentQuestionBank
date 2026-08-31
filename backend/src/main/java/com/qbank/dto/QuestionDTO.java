package com.qbank.dto;

import java.util.List;

/**
 * 题目出入参(选项以数组传输, 服务层负责与JSON字符串互转)
 */
public class QuestionDTO {
    private Long id;
    private Long categoryId;
    private Long bankId;
    private Integer type;
    private String title;
    private List<String> options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private String tags;
    private String source;

    /** 非数据库字段: 题目所属用户(前端据此判断归属/编辑/删除权限) */
    private Long userId;
    /** 非数据库字段: 分类名称/题库名称/收藏状态(查询返回) */
    private String categoryName;
    private String bankName;
    private Boolean favorited;
    /** 非数据库字段: 共享状态 - sharedByMe: 1我共享过; incomingPermission: 收到权限 1只读 2可编辑(null=未收到) */
    private Integer sharedByMe;
    private Integer incomingPermission;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
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
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
    public Integer getSharedByMe() { return sharedByMe; }
    public void setSharedByMe(Integer sharedByMe) { this.sharedByMe = sharedByMe; }
    public Integer getIncomingPermission() { return incomingPermission; }
    public void setIncomingPermission(Integer incomingPermission) { this.incomingPermission = incomingPermission; }
}