package com.qbank.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 练习会题目快照
 */
public interface PracticeQuestionMapper {

    void insertBatch(@Param("recordId") Long recordId, @Param("questionIds") List<Long> questionIds);

    List<Long> selectQuestionIdsByRecord(@Param("recordId") Long recordId);
}
