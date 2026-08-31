package com.qbank.mapper;

import com.qbank.dto.NameValueVO;
import com.qbank.dto.QuestionQuery;
import com.qbank.entity.Question;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QuestionMapper {

    int insert(Question question);

    int insertBatch(@Param("list") List<Question> list);

    int update(Question question);

    int deleteByIds(@Param("ids") List<Long> ids);

    Question findById(@Param("id") Long id);

    /** 分页查询; includeShared=true 时(普通用户未按题库过滤)同时包含收到的共享题目 */
    List<Question> selectPage(@Param("userId") Long userId, @Param("q") QuestionQuery query,
                              @Param("includeShared") boolean includeShared);

    List<Question> selectByIds(@Param("ids") List<Long> ids);

    /** 顺序练习: 按 id 升序取前 limit 条 */
    List<Question> selectForPractice(@Param("userId") Long userId,
                                     @Param("categoryId") Long categoryId,
                                     @Param("bankId") Long bankId,
                                     @Param("difficulty") Integer difficulty,
                                     @Param("type") Integer type,
                                     @Param("limit") int limit,
                                     @Param("onlyWrong") boolean onlyWrong);

    /** 随机练习候选 id: 先取候选 id 列表(上限 limit), 由 Java 侧随机取数, 避免全表 ORDER BY RAND() */
    List<Long> selectPracticeCandidateIds(@Param("userId") Long userId,
                                          @Param("categoryId") Long categoryId,
                                          @Param("bankId") Long bankId,
                                          @Param("difficulty") Integer difficulty,
                                          @Param("type") Integer type,
                                          @Param("limit") int limit,
                                          @Param("onlyWrong") boolean onlyWrong);

    int countByUser(@Param("userId") Long userId);

    /** 练习筛选条件下的题目总数(与抽题同条件) */
    int countForPractice(@Param("userId") Long userId,
                         @Param("categoryId") Long categoryId,
                         @Param("bankId") Long bankId,
                         @Param("difficulty") Integer difficulty,
                         @Param("type") Integer type,
                         @Param("onlyWrong") boolean onlyWrong);

    int countByBank(@Param("bankId") Long bankId);

    List<Question> selectByBank(@Param("bankId") Long bankId);

    int clearBank(@Param("bankId") Long bankId);

    List<NameValueVO> countByType(@Param("userId") Long userId);

    List<NameValueVO> countByDifficulty(@Param("userId") Long userId);

    List<NameValueVO> countByCategory(@Param("userId") Long userId);
}