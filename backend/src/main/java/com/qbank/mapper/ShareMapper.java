package com.qbank.mapper;

import com.qbank.entity.Share;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShareMapper {

    int insert(Share share);

    int delete(@Param("id") Long id, @Param("fromUserId") Long fromUserId);

    int deleteByQuestionIds(@Param("ids") List<Long> ids);

    Share findById(@Param("id") Long id);

    List<Share> selectSent(@Param("fromUserId") Long fromUserId);

    List<Share> selectReceived(@Param("userId") Long userId);

    /** 指定用户能否访问题目(接收人或公开共享) */
    int countAccessible(@Param("questionId") Long questionId, @Param("userId") Long userId);

    /** 题目是否已有公开共享 */
    int countPublic(@Param("questionId") Long questionId, @Param("fromUserId") Long fromUserId);
}
