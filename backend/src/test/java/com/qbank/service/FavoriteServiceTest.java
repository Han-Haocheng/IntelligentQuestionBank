package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.entity.Favorite;
import com.qbank.entity.Question;
import com.qbank.mapper.FavoriteMapper;
import com.qbank.mapper.QuestionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收藏服务测试: 切换收藏 / 分页 / 移除
 */
class FavoriteServiceTest {

    @AfterEach
    void clearPageHelper() {
        PageHelper.clearPage();
    }

    private FavoriteService newService(FavoriteMapper fm, QuestionMapper qm) {
        return new FavoriteService(fm, qm);
    }

    @Test
    void toggleQuestionMissingThrows() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(questionMapper.findById(9L)).thenReturn(null);
        FavoriteMapper favoriteMapper = mock(FavoriteMapper.class);
        FavoriteService service = newService(favoriteMapper, questionMapper);
        assertThatThrownBy(() -> service.toggle(7L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("题目不存在");
        verify(favoriteMapper, never()).insert(anyLong(), anyLong());
        verify(favoriteMapper, never()).delete(anyLong(), anyLong());
    }

    @Test
    void toggleExistingCancels() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(questionMapper.findById(9L)).thenReturn(new Question());
        FavoriteMapper favoriteMapper = mock(FavoriteMapper.class);
        when(favoriteMapper.find(7L, 9L)).thenReturn(new Favorite());
        FavoriteService service = newService(favoriteMapper, questionMapper);
        assertThat(service.toggle(7L, 9L)).isFalse();
        verify(favoriteMapper).delete(7L, 9L);
        verify(favoriteMapper, never()).insert(anyLong(), anyLong());
    }

    @Test
    void toggleNewInserts() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(questionMapper.findById(9L)).thenReturn(new Question());
        FavoriteMapper favoriteMapper = mock(FavoriteMapper.class);
        when(favoriteMapper.find(7L, 9L)).thenReturn(null);
        FavoriteService service = newService(favoriteMapper, questionMapper);
        assertThat(service.toggle(7L, 9L)).isTrue();
        verify(favoriteMapper).insert(7L, 9L);
        verify(favoriteMapper, never()).delete(anyLong(), anyLong());
    }

    @Test
    void pageDelegates() {
        Question q = new Question();
        q.setId(1L);
        FavoriteMapper favoriteMapper = mock(FavoriteMapper.class);
        when(favoriteMapper.selectPage(7L)).thenReturn(List.of(q));
        FavoriteService service = newService(favoriteMapper, mock(QuestionMapper.class));
        PageInfo<Question> page = service.page(7L, 1, 10);
        assertThat(page.getList()).hasSize(1);
        verify(favoriteMapper).selectPage(7L);
    }

    @Test
    void removeDeletes() {
        FavoriteMapper favoriteMapper = mock(FavoriteMapper.class);
        FavoriteService service = newService(favoriteMapper, mock(QuestionMapper.class));
        service.remove(7L, 3L);
        verify(favoriteMapper).delete(7L, 3L);
    }
}
