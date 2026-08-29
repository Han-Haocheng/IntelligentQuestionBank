package com.qbank.dto;

/**
 * 统计总览
 */
public class OverviewVO {
    private Long questionCount;
    private Long categoryCount;
    private Long favoriteCount;
    private Long wrongCount;
    private Long practiceCount;
    private Double accuracy;     // 总正确率(百分比)

    public Long getQuestionCount() { return questionCount; }
    public void setQuestionCount(Long questionCount) { this.questionCount = questionCount; }
    public Long getCategoryCount() { return categoryCount; }
    public void setCategoryCount(Long categoryCount) { this.categoryCount = categoryCount; }
    public Long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Long favoriteCount) { this.favoriteCount = favoriteCount; }
    public Long getWrongCount() { return wrongCount; }
    public void setWrongCount(Long wrongCount) { this.wrongCount = wrongCount; }
    public Long getPracticeCount() { return practiceCount; }
    public void setPracticeCount(Long practiceCount) { this.practiceCount = practiceCount; }
    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
}
