package com.qbank.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目分类(支持二级树)
 */
public class Category {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;
    private Long userId;
    private LocalDateTime createTime;

    /** 非数据库字段: 树形子分类 */
    private List<Category> children;
    /** 非数据库字段: 题目数量 */
    private Long questionCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public List<Category> getChildren() { return children; }
    public void setChildren(List<Category> children) { this.children = children; }
    public Long getQuestionCount() { return questionCount; }
    public void setQuestionCount(Long questionCount) { this.questionCount = questionCount; }
}
