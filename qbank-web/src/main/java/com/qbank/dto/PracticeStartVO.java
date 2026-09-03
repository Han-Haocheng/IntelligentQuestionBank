package com.qbank.dto;

import com.qbank.entity.PracticeRecord;

import java.util.List;

/**
 * 开始练习返回: 记录 + 题目(不含答案)
 */
public class PracticeStartVO {
    private PracticeRecord record;
    private List<QuestionDTO> questions;

    public PracticeRecord getRecord() { return record; }
    public void setRecord(PracticeRecord record) { this.record = record; }
    public List<QuestionDTO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDTO> questions) { this.questions = questions; }
}
