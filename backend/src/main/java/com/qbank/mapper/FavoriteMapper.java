package com.qbank.mapper;

import com.qbank.entity.Favorite;
import com.qbank.entity.Question;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FavoriteMapper {

    int insert(@Param("userId") Long userId, @Param("questionId") Long questionId);

    int delete(@Param("userId") Long userId, @Param("questionId") Long questionId);

    Favorite find(@Param("userId") Long userId, @Param("questionId") Long questionId);

    List<Question> selectPage(@Param("userId") Long userId);

    int countByUser(@Param("userId") Long userId);

    int deleteByQuestionIds(@Param("ids") List<Long> ids);
}
