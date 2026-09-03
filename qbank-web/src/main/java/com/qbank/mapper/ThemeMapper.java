package com.qbank.mapper;

import com.qbank.entity.Theme;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ThemeMapper {

    List<Theme> selectAll();

    List<Theme> selectEnabled();

    Theme findById(@Param("id") Long id);

    Theme findByKey(@Param("themeKey") String themeKey);

    Theme findDefault();

    int count();

    int insert(Theme theme);

    int update(Theme theme);

    int updateStatus(@Param("id") Long id, @Param("enabled") Integer enabled);

    int clearDefault();

    int setDefault(@Param("id") Long id);

    int deleteById(@Param("id") Long id);
}