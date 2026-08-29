package com.qbank.mapper;

import com.qbank.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CategoryMapper {

    int insert(Category category);

    int update(Category category);

    int deleteById(@Param("id") Long id);

    Category findById(@Param("id") Long id);

    List<Category> selectByUser(@Param("userId") Long userId);

    int countChildren(@Param("userId") Long userId, @Param("parentId") Long parentId);

    int countQuestions(@Param("categoryId") Long categoryId);

    int countByUser(@Param("userId") Long userId);
}
