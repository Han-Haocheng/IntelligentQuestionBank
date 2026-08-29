package com.qbank.mapper;

import com.qbank.dto.NameValueVO;
import com.qbank.dto.QuestionQuery;
import com.qbank.entity.Question;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QuestionMapper {

    int insert(Question question);

    int update(Question question);

    int deleteByIds(@Param("ids") List<Long> ids);

    Question findById(@Param("id") Long id);

    List<Question> selectPage(@Param("userId") Long userId, @Param("q") QuestionQuery query);

    List<Question> selectByIds(@Param("ids") List<Long> ids);

    List<Question> selectForPractice(@Param("userId") Long userId,
                                     @Param("categoryId") Long categoryId,
                                     @Param("bankId") Long bankId,
                                     @Param("difficulty") Integer difficulty,
                                     @Param("type") Integer type,
                                     @Param("limit") int limit,
                                     @Param("random") boolean random,
                                     @Param("onlyWrong") boolean onlyWrong);

    int countByUser(@Param("userId") Long userId);

    int countByBank(@Param("bankId") Long bankId);

    int clearBank(@Param("bankId") Long bankId);

    List<NameValueVO> countByType(@Param("userId") Long userId);

    List<NameValueVO> countByDifficulty(@Param("userId") Long userId);

    List<NameValueVO> countByCategory(@Param("userId") Long userId);
}
