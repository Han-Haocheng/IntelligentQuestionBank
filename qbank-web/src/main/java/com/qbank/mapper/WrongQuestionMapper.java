package com.qbank.mapper;

import com.qbank.dto.NameValueVO;
import com.qbank.entity.WrongQuestion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WrongQuestionMapper {

    WrongQuestion find(@Param("userId") Long userId, @Param("questionId") Long questionId);

    int insert(WrongQuestion wrongQuestion);

    int incrementWrong(@Param("id") Long id, @Param("lastAnswer") String lastAnswer);

    int updateMastered(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("mastered") Integer mastered);

    int delete(@Param("userId") Long userId, @Param("questionId") Long questionId);

    int deleteByQuestionIds(@Param("ids") List<Long> ids);

    List<WrongQuestion> selectPage(@Param("userId") Long userId,
                                   @Param("mastered") Integer mastered,
                                   @Param("categoryId") Long categoryId);

    int countByUser(@Param("userId") Long userId, @Param("mastered") Integer mastered);

    /** 批量查询某批题目的错题记录 */
    List<WrongQuestion> findByUserAndQuestionIds(@Param("userId") Long userId, @Param("questionIds") List<Long> questionIds);

    List<NameValueVO> countGroupByCategory(@Param("userId") Long userId);
}
