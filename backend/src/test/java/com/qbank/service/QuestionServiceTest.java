package com.qbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qbank.common.BusinessException;
import com.qbank.dto.QuestionDTO;
import com.qbank.entity.Category;
import com.qbank.entity.Question;
import com.qbank.mapper.BankMapper;
import com.qbank.mapper.CategoryMapper;
import com.qbank.mapper.FavoriteMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.ShareMapper;
import com.qbank.mapper.WrongQuestionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题目服务: 分类归属越权校验测试
 */
class QuestionServiceTest {

    private QuestionService newService(CategoryMapper categoryMapper) {
        return new QuestionService(mock(QuestionMapper.class), categoryMapper, mock(BankMapper.class),
                mock(FavoriteMapper.class), mock(ShareMapper.class), mock(WrongQuestionMapper.class),
                new ObjectMapper());
    }

    private QuestionDTO validDTO() {
        QuestionDTO dto = new QuestionDTO();
        dto.setTitle("测试题目");
        dto.setType(1);
        dto.setDifficulty(1);
        dto.setOptions(List.of("A", "B"));
        dto.setAnswer("A");
        dto.setCategoryId(1L);
        return dto;
    }

    @Test
    void addRejectsCategoryOwnedByAnotherUser() {
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        Category foreign = new Category();
        foreign.setId(1L);
        foreign.setUserId(99L);
        when(categoryMapper.findById(1L)).thenReturn(foreign);

        QuestionService service = newService(categoryMapper);
        assertThatThrownBy(() -> service.add(7L, validDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权使用");
    }

    @Test
    void addAcceptsOwnCategory() {
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        Category own = new Category();
        own.setId(1L);
        own.setUserId(7L);
        when(categoryMapper.findById(1L)).thenReturn(own);

        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionService service = new QuestionService(questionMapper, categoryMapper, mock(BankMapper.class),
                mock(FavoriteMapper.class), mock(ShareMapper.class), mock(WrongQuestionMapper.class),
                new ObjectMapper());
        service.add(7L, validDTO());
        verify(questionMapper).insert(any(Question.class));
    }
}
