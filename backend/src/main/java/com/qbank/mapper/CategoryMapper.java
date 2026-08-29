package com.qbank.mapper;

import com.qbank.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CategoryMapper {

    int insert(Category category);

    int update(Category category);

    int deleteById(@Param("id") Long id);

    Category findById(@Param("id") Long id);

    /** 全局分类(所有用户共用) */
    List<Category> selectAll();

    List<Category> selectChildren(@Param("parentId") Long parentId);

    int countChildren(@Param("parentId") Long parentId);

    int countQuestions(@Param("categoryId") Long categoryId);

    int countAll();

    /** 更新排序号(同级拖拽排序) */
    int updateSort(@Param("id") Long id, @Param("sort") int sort);

    /** 该分类及子级下的题目总数(删除影响面/合并统计) */
    int countQuestionsInSubtree(@Param("categoryId") Long categoryId);

    /** 把该分类及子级下的题目迁移到目标分类 */
    int moveQuestions(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
