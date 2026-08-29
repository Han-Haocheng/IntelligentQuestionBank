package com.qbank.mapper;

import com.qbank.dto.TrendVO;
import com.qbank.entity.PracticeRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PracticeRecordMapper {

    int insert(PracticeRecord record);

    int updateFinish(PracticeRecord record);

    PracticeRecord findById(@Param("id") Long id);

    List<PracticeRecord> selectPage(@Param("userId") Long userId);

    int deleteById(@Param("id") Long id);

    int countByUser(@Param("userId") Long userId);

    /** 已完成练习的合计(total/correct) */
    PracticeRecord selectSumStats(@Param("userId") Long userId);

    /** 近14天练习趋势 */
    List<TrendVO> selectTrend(@Param("userId") Long userId);
}
