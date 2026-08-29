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

    int countChildren(@Param("parentId") Long parentId);

    int countQuestions(@Param("categoryId") Long categoryId);

    int countAll();
}
