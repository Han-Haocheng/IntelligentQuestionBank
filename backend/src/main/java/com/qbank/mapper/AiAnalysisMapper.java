package com.qbank.mapper;

import com.qbank.entity.AiAnalysis;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiAnalysisMapper {

    int insert(AiAnalysis analysis);

    List<AiAnalysis> selectPage(@Param("userId") Long userId);

    int countByUser(@Param("userId") Long userId);
}
