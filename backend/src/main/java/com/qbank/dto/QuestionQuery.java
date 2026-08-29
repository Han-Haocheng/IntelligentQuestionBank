package com.qbank.dto;

/**
 * 题目分页查询入参
 */
public class QuestionQuery {
    private String keyword;
    private Long categoryId;
    private Long bankId;
    private Integer type;
    private Integer difficulty;
    private String tag;
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public Integer getPageNum() { return com.qbank.common.PageUtil.pageNum(pageNum == null ? 1 : pageNum); }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return com.qbank.common.PageUtil.pageSize(pageSize == null ? 10 : pageSize); }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
