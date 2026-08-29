package com.qbank.entity;

import java.time.LocalDateTime;

/**
 * 错题本
 */
public class WrongQuestion {

    private Long id;
    private Long userId;
    private Long questionId;
    private Integer wrongCount;
    private String lastAnswer;
    private Integer mastered;    // 0未掌握 1已掌握
    private LocalDateTime createTime;
    private LocalDateTime lastWrongTime;

    /** 非数据库字段(联表查询) */
    private String title;
    private Integer type;
    private Integer difficulty;
    private Long categoryId;
    private String categoryName;
    private String answer;
    private String analysis;
    private String tags;
    private Boolean favorited;   // 当前用户是否已收藏该题

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
    public String getLastAnswer() { return lastAnswer; }
    public void setLastAnswer(String lastAnswer) { this.lastAnswer = lastAnswer; }
    public Integer getMastered() { return mastered; }
    public void setMastered(Integer mastered) { this.mastered = mastered; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getLastWrongTime() { return lastWrongTime; }
    public void setLastWrongTime(LocalDateTime lastWrongTime) { this.lastWrongTime = lastWrongTime; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
}
