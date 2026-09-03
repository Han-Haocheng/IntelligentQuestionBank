package com.qbank.mapper;

import com.qbank.entity.Share;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShareMapper {

    int insert(Share share);

    /** 公开共享条件插入(原子防重): 返回 0 表示已存在同类公开共享 */
    int insertPublic(Share share);

    int delete(@Param("id") Long id, @Param("fromUserId") Long fromUserId);

    int deleteByQuestionIds(@Param("ids") List<Long> ids);

    Share findById(@Param("id") Long id);

    List<Share> selectSent(@Param("fromUserId") Long fromUserId);

    List<Share> selectReceived(@Param("userId") Long userId);

    /** 指定用户能否访问题目(接收人或公开共享) */
    int countAccessible(@Param("questionId") Long questionId, @Param("userId") Long userId);

    /** 指定用户能否访问题库 */
    int countBankAccessible(@Param("bankId") Long bankId, @Param("userId") Long userId);

    /** 指定用户对该题目是否拥有可编辑共享 */
    int countEditable(@Param("questionId") Long questionId, @Param("userId") Long userId);

    /** 指定用户对该题库是否拥有可编辑共享 */
    int countBankEditable(@Param("bankId") Long bankId, @Param("userId") Long userId);

    int updatePermission(@Param("id") Long id, @Param("permission") Integer permission);

    /** 题目是否已有公开共享 */
    int countPublic(@Param("questionId") Long questionId, @Param("fromUserId") Long fromUserId);

    /** 题库是否已有公开共享 */
    int countBankPublic(@Param("bankId") Long bankId, @Param("fromUserId") Long fromUserId);

    int deleteByBank(@Param("bankId") Long bankId);
}
