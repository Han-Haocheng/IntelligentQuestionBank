package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.entity.WrongQuestion;
import com.qbank.mapper.FavoriteMapper;
import com.qbank.mapper.WrongQuestionMapper;
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
 * 错题本服务测试: 分页收藏标记 / 掌握状态翻转 / 删除
 */
class WrongQuestionServiceTest {

    @AfterEach
    void clearPageHelper() {
        PageHelper.clearPage();
    }

    private WrongQuestion wrong(long id, long questionId, Integer mastered) {
        WrongQuestion w = new WrongQuestion();
        w.setId(id);
        w.setQuestionId(questionId);
        w.setMastered(mastered);
        return w;
    }

    @Test
    void pageFillsFavoritedStatus() {
        WrongQuestionMapper wrongMapper = mock(WrongQuestionMapper.class);
        FavoriteMapper favoriteMapper = mock(FavoriteMapper.class);
        when(wrongMapper.selectPage(7L, null, null)).thenReturn(
                List.of(wrong(1L, 10L, 0), wrong(2L, 11L, 1)));
        when(favoriteMapper.selectIdsByUserAndQuestionIds(7L, List.of(10L, 11L)))
                .thenReturn(List.of(10L));

        WrongQuestionService service = new WrongQuestionService(wrongMapper, favoriteMapper);
        PageInfo<WrongQuestion> page = service.page(7L, null, null, 1, 10);
        assertThat(page.getList()).hasSize(2);
        assertThat(page.getList().get(0).getFavorited()).isTrue();
        assertThat(page.getList().get(1).getFavorited()).isFalse();
    }

    @Test
    void pageEmptyListOk() {
        WrongQuestionMapper wrongMapper = mock(WrongQuestionMapper.class);
        when(wrongMapper.selectPage(7L, 1, 3L)).thenReturn(List.of());
        WrongQuestionService service = new WrongQuestionService(wrongMapper, mock(FavoriteMapper.class));
        PageInfo<WrongQuestion> page = service.page(7L, 1, 3L, 2, 20);
        assertThat(page.getList()).isEmpty();
        verify(wrongMapper).selectPage(7L, 1, 3L);
    }

    @Test
    void toggleMasterMissingThrows() {
        WrongQuestionMapper wrongMapper = mock(WrongQuestionMapper.class);
        when(wrongMapper.find(7L, 10L)).thenReturn(null);
        WrongQuestionService service = new WrongQuestionService(wrongMapper, mock(FavoriteMapper.class));
        assertThatThrownBy(() -> service.toggleMaster(7L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("错题记录不存在");
        verify(wrongMapper, never()).updateMastered(anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void toggleMasterMarksMastered() {
        WrongQuestionMapper wrongMapper = mock(WrongQuestionMapper.class);
        when(wrongMapper.find(7L, 10L)).thenReturn(wrong(1L, 10L, 0));
        WrongQuestionService service = new WrongQuestionService(wrongMapper, mock(FavoriteMapper.class));
        assertThat(service.toggleMaster(7L, 10L)).isEqualTo(1);
        verify(wrongMapper).updateMastered(7L, 10L, 1);
    }

    @Test
    void toggleMasterUnmarks() {
        WrongQuestionMapper wrongMapper = mock(WrongQuestionMapper.class);
        when(wrongMapper.find(7L, 10L)).thenReturn(wrong(1L, 10L, 1));
        WrongQuestionService service = new WrongQuestionService(wrongMapper, mock(FavoriteMapper.class));
        assertThat(service.toggleMaster(7L, 10L)).isEqualTo(0);
        verify(wrongMapper).updateMastered(7L, 10L, 0);
    }

    @Test
    void deleteDeletes() {
        WrongQuestionMapper wrongMapper = mock(WrongQuestionMapper.class);
        WrongQuestionService service = new WrongQuestionService(wrongMapper, mock(FavoriteMapper.class));
        service.delete(7L, 10L);
        verify(wrongMapper).delete(7L, 10L);
    }
}
