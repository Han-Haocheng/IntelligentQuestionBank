package com.qbank.mapper;

import com.qbank.entity.PracticeAnswer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PracticeAnswerMapper {

    int insertBatch(@Param("list") List<PracticeAnswer> list);

    List<PracticeAnswer> selectByRecord(@Param("recordId") Long recordId);

    int deleteByRecord(@Param("recordId") Long recordId);
}
