package com.qbank.dto;

/**
 * 开始练习入参
 */
public class PracticeStartDTO {
    private String name;
    private Integer mode = 1;       // 1顺序 2随机 3错题重做
    private Long categoryId;
    private Integer difficulty;
    private Integer type;
    private Integer count = 10;
    private Boolean onlyWrong = false;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getMode() { return mode == null ? 1 : mode; }
    public void setMode(Integer mode) { this.mode = mode; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getCount() { return count == null ? 10 : count; }
    public void setCount(Integer count) { this.count = count; }
    public Boolean getOnlyWrong() { return onlyWrong != null && onlyWrong; }
    public void setOnlyWrong(Boolean onlyWrong) { this.onlyWrong = onlyWrong; }
}
