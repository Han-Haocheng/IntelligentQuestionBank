package com.qbank.service;

import com.qbank.dto.NameValueVO;
import com.qbank.dto.OverviewVO;
import com.qbank.dto.TrendVO;
import com.qbank.entity.PracticeRecord;
import com.qbank.mapper.CategoryMapper;
import com.qbank.mapper.FavoriteMapper;
import com.qbank.mapper.PracticeRecordMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.UserMapper;
import com.qbank.mapper.WrongQuestionMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 统计服务测试: 作用域/正确率计算/趋势补零
 */
class StatsServiceTest {

    private StatsService newService(QuestionMapper qm, CategoryMapper cm, FavoriteMapper fm,
                                    WrongQuestionMapper wm, PracticeRecordMapper pm, UserMapper um) {
        return new StatsService(qm, cm, fm, wm, pm, um);
    }

    private PracticeRecord sumStats(Long total, Long correct) {
        PracticeRecord r = new PracticeRecord();
        r.setTotal(total == null ? null : total.intValue());
        r.setCorrect(correct == null ? null : correct.intValue());
        return r;
    }

    @Test
    void overviewNormalUserUsesOwnScope() {
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.countByUser(7L)).thenReturn(10);
        CategoryMapper cm = mock(CategoryMapper.class);
        when(cm.countAll()).thenReturn(3);
        FavoriteMapper fm = mock(FavoriteMapper.class);
        when(fm.countByUser(7L)).thenReturn(2);
        WrongQuestionMapper wm = mock(WrongQuestionMapper.class);
        when(wm.countByUser(7L, null)).thenReturn(1);
        PracticeRecordMapper pm = mock(PracticeRecordMapper.class);
        when(pm.countByUser(7L)).thenReturn(4);
        when(pm.selectSumStats(7L)).thenReturn(sumStats(3L, 1L));
        UserMapper um = mock(UserMapper.class);

        StatsService service = newService(qm, cm, fm, wm, pm, um);
        OverviewVO vo = service.overview(7L, 1, null);
        assertThat(vo.getQuestionCount()).isEqualTo(10);
        assertThat(vo.getCategoryCount()).isEqualTo(3);
        assertThat(vo.getFavoriteCount()).isEqualTo(2);
        assertThat(vo.getWrongCount()).isEqualTo(1);
        assertThat(vo.getPracticeCount()).isEqualTo(4);
        assertThat(vo.getAccuracy()).isEqualTo(33.3);
        assertThat(vo.getUserCount()).isNull();
        verify(um, never()).countByUser(anyLong());
    }

    @Test
    void overviewAdminWithoutTargetCountsAll() {
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.countByUser(null)).thenReturn(50);
        CategoryMapper cm = mock(CategoryMapper.class);
        when(cm.countAll()).thenReturn(5);
        FavoriteMapper fm = mock(FavoriteMapper.class);
        when(fm.countByUser(null)).thenReturn(20);
        WrongQuestionMapper wm = mock(WrongQuestionMapper.class);
        when(wm.countByUser(null, null)).thenReturn(8);
        PracticeRecordMapper pm = mock(PracticeRecordMapper.class);
        when(pm.countByUser(null)).thenReturn(30);
        when(pm.selectSumStats(null)).thenReturn(null);
        UserMapper um = mock(UserMapper.class);
        when(um.countByUser(null)).thenReturn(6);

        StatsService service = newService(qm, cm, fm, wm, pm, um);
        OverviewVO vo = service.overview(1L, 0, null);
        assertThat(vo.getAccuracy()).isEqualTo(0.0);
        assertThat(vo.getUserCount()).isEqualTo(6);
        verify(qm).countByUser(null);
    }

    @Test
    void overviewAdminWithTargetUsesTargetScope() {
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.countByUser(5L)).thenReturn(7);
        StatsService service = newService(qm, mock(CategoryMapper.class),
                mock(FavoriteMapper.class), mock(WrongQuestionMapper.class),
                mock(PracticeRecordMapper.class), mock(UserMapper.class));
        OverviewVO vo = service.overview(1L, 0, 5L);
        assertThat(vo.getQuestionCount()).isEqualTo(7);
    }

    @Test
    void overviewAccuracyRounding() {
        PracticeRecordMapper pm = mock(PracticeRecordMapper.class);
        when(pm.selectSumStats(7L)).thenReturn(sumStats(3L, 2L));
        StatsService service = newService(mock(QuestionMapper.class), mock(CategoryMapper.class),
                mock(FavoriteMapper.class), mock(WrongQuestionMapper.class), pm, mock(UserMapper.class));
        OverviewVO vo = service.overview(7L, 1, null);
        // 2/3 = 0.6666... → 66.7
        assertThat(vo.getAccuracy()).isEqualTo(66.7);
    }

    @Test
    void byTypeOrdersAndSkipsMissing() {
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.countByType(7L)).thenReturn(
                List.of(new NameValueVO("3", 2L), new NameValueVO("1", 5L)));
        StatsService service = newService(qm, mock(CategoryMapper.class), mock(FavoriteMapper.class),
                mock(WrongQuestionMapper.class), mock(PracticeRecordMapper.class), mock(UserMapper.class));
        List<NameValueVO> result = service.byType(7L, 1, null);
        assertThat(result).extracting(NameValueVO::getName)
                .containsExactly("单选题", "填空题");
        assertThat(result).extracting(NameValueVO::getValue).containsExactly(5L, 2L);
    }

    @Test
    void byDifficultyOrdersAndSkipsMissing() {
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.countByDifficulty(7L)).thenReturn(
                List.of(new NameValueVO("4", 1L), new NameValueVO("2", 3L)));
        StatsService service = newService(qm, mock(CategoryMapper.class), mock(FavoriteMapper.class),
                mock(WrongQuestionMapper.class), mock(PracticeRecordMapper.class), mock(UserMapper.class));
        List<NameValueVO> result = service.byDifficulty(7L, 1, null);
        assertThat(result).extracting(NameValueVO::getName)
                .containsExactly("简单", "较难");
        assertThat(result).extracting(NameValueVO::getValue).containsExactly(3L, 1L);
    }

    @Test
    void byCategoryDelegates() {
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.countByCategory(7L)).thenReturn(List.of(new NameValueVO("Java", 4L)));
        StatsService service = newService(qm, mock(CategoryMapper.class), mock(FavoriteMapper.class),
                mock(WrongQuestionMapper.class), mock(PracticeRecordMapper.class), mock(UserMapper.class));
        assertThat(service.byCategory(7L, 1, null)).hasSize(1);
    }

    @Test
    void wrongByCategoryDelegates() {
        WrongQuestionMapper wm = mock(WrongQuestionMapper.class);
        when(wm.countGroupByCategory(7L)).thenReturn(List.of(new NameValueVO("Java", 2L)));
        StatsService service = newService(mock(QuestionMapper.class), mock(CategoryMapper.class),
                mock(FavoriteMapper.class), wm, mock(PracticeRecordMapper.class), mock(UserMapper.class));
        assertThat(service.wrongByCategory(7L, 1, null)).hasSize(1);
    }

    @Test
    void trendZeroFills14Days() {
        PracticeRecordMapper pm = mock(PracticeRecordMapper.class);
        when(pm.selectTrend(7L)).thenReturn(List.of());
        StatsService service = newService(mock(QuestionMapper.class), mock(CategoryMapper.class),
                mock(FavoriteMapper.class), mock(WrongQuestionMapper.class), pm, mock(UserMapper.class));
        List<TrendVO> result = service.trend(7L, 1, null);
        assertThat(result).hasSize(14);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.now().minusDays(13).format(fmt));
        assertThat(result.get(13).getDate()).isEqualTo(LocalDate.now().format(fmt));
        assertThat(result).allMatch(t -> t.getTotal() == 0 && t.getCorrect() == 0 && t.getAccuracy() == 0.0);
    }

    @Test
    void trendKeepsExistingAndComputesAccuracy() {
        PracticeRecordMapper pm = mock(PracticeRecordMapper.class);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String past = LocalDate.now().minusDays(3).format(fmt);
        TrendVO item = new TrendVO(past, 4L, 2L, 0.0);
        when(pm.selectTrend(7L)).thenReturn(List.of(item));
        StatsService service = newService(mock(QuestionMapper.class), mock(CategoryMapper.class),
                mock(FavoriteMapper.class), mock(WrongQuestionMapper.class), pm, mock(UserMapper.class));
        List<TrendVO> result = service.trend(7L, 1, null);
        assertThat(result).hasSize(14);
        TrendVO hit = result.stream().filter(t -> past.equals(t.getDate())).findFirst().orElseThrow();
        assertThat(hit.getTotal()).isEqualTo(4);
        assertThat(hit.getCorrect()).isEqualTo(2);
        assertThat(hit.getAccuracy()).isEqualTo(50.0);
    }
}
