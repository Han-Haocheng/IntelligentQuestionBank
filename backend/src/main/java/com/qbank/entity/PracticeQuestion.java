package com.qbank.entity;

/**
 * 练习会题目快照
 */
public class PracticeQuestion {

    private Long id;
    private Long recordId;
    private Long questionId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
}
