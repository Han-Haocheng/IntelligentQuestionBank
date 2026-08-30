package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.dto.PracticeStartDTO;
import com.qbank.dto.PracticeStartVO;
import com.qbank.dto.PracticeSubmitDTO;
import com.qbank.dto.QuestionDTO;
import com.qbank.entity.PracticeAnswer;
import com.qbank.entity.PracticeRecord;
import com.qbank.entity.Question;
import com.qbank.entity.WrongQuestion;
import com.qbank.mapper.PracticeAnswerMapper;
import com.qbank.mapper.PracticeQuestionMapper;
import com.qbank.mapper.PracticeRecordMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.WrongQuestionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 练习服务测试: 抽题/快照/防作弊, 交卷判分与错题本维护
 */
class PracticeServiceTest {

    @AfterEach
    void clearPageHelper() {
        PageHelper.clearPage();
    }

    private PracticeService newService(PracticeRecordMapper rm, PracticeAnswerMapper am,
                                       PracticeQuestionMapper pqm, QuestionMapper qm,
                                       WrongQuestionMapper wm, QuestionService qs) {
        return new PracticeService(rm, am, pqm, qm, wm, qs);
    }

    private Question question(long id, long userId, int type, String answer) {
        Question q = new Question();
        q.setId(id);
        q.setUserId(userId);
        q.setType(type);
        q.setAnswer(answer);
        q.setTitle("题" + id);
        return q;
    }

    private QuestionDTO dtoOf(Question q) {
        QuestionDTO d = new QuestionDTO();
        d.setId(q.getId());
        d.setAnswer(q.getAnswer());
        d.setAnalysis("解析");
        d.setTitle(q.getTitle());
        return d;
    }

    private PracticeStartDTO startDTO() {
        PracticeStartDTO d = new PracticeStartDTO();
        d.setCount(10);
        return d;
    }

    private PracticeRecord record(long id, long userId, Integer status, Integer mode, Integer total) {
        PracticeRecord r = new PracticeRecord();
        r.setId(id);
        r.setUserId(userId);
        r.setStatus(status);
        r.setMode(mode);
        r.setTotal(total);
        return r;
    }

    // ==================== start ====================

    @Test
    void startClampsCount() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.selectForPractice(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.of(question(1L, 7L, 1, "A")));
        QuestionService qs = mock(QuestionService.class);
        when(qs.toDTO(any(Question.class))).thenReturn(new QuestionDTO());
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), qm, mock(WrongQuestionMapper.class), qs);

        PracticeStartDTO small = startDTO();
        small.setCount(0);
        service.start(7L, small);
        verify(qm).selectForPractice(any(), any(), any(), any(), any(), eq(1), anyBoolean());

        PracticeStartDTO large = startDTO();
        large.setCount(100);
        service.start(7L, large);
        verify(qm).selectForPractice(any(), any(), any(), any(), any(), eq(50), anyBoolean());
    }

    @Test
    void startNoQuestionsThrows() {
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.selectForPractice(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.of());
        PracticeService service = newService(mock(PracticeRecordMapper.class),
                mock(PracticeAnswerMapper.class), mock(PracticeQuestionMapper.class),
                qm, mock(WrongQuestionMapper.class), mock(QuestionService.class));
        assertThatThrownBy(() -> service.start(7L, startDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有符合条件的题目");
    }

    @Test
    void startSequenceUsesSelectForPracticeAndSnapshots() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.selectForPractice(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.of(question(1L, 7L, 1, "A")));
        PracticeQuestionMapper pqm = mock(PracticeQuestionMapper.class);
        QuestionService qs = mock(QuestionService.class);
        when(qs.toDTO(any(Question.class))).thenReturn(new QuestionDTO());
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                pqm, qm, mock(WrongQuestionMapper.class), qs);

        PracticeStartVO vo = service.start(7L, startDTO());
        verify(rm).insert(any(PracticeRecord.class));
        verify(pqm).insertBatch(any(), anyList());
        assertThat(vo.getQuestions()).hasSize(1);
    }

    @Test
    void startRandomPicksFromCandidates() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        // 返回可变列表: pickRandomQuestions 中会执行 Collections.shuffle
        when(qm.selectPracticeCandidateIds(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(new java.util.ArrayList<>(List.of(1L, 2L, 3L)));
        when(qm.selectByIds(anyList())).thenAnswer(inv -> inv.getArgument(0, List.class).stream()
                .map(id -> question((Long) id, 7L, 1, "A")).toList());
        PracticeQuestionMapper pqm = mock(PracticeQuestionMapper.class);
        QuestionService qs = mock(QuestionService.class);
        when(qs.toDTO(any(Question.class))).thenReturn(new QuestionDTO());
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                pqm, qm, mock(WrongQuestionMapper.class), qs);

        PracticeStartDTO dto = startDTO();
        dto.setMode(2); // 随机
        dto.setCount(2);
        PracticeStartVO vo = service.start(7L, dto);
        verify(qm).selectPracticeCandidateIds(eq(7L), any(), any(), any(), any(), eq(500), anyBoolean());
        verify(pqm).insertBatch(any(), anyList());
        assertThat(vo.getQuestions()).hasSize(2);
    }

    @Test
    void startHidesAnswerAndAnalysis() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.selectForPractice(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.of(question(1L, 7L, 1, "A")));
        QuestionService qs = mock(QuestionService.class);
        when(qs.toDTO(any(Question.class))).thenReturn(dtoOf(question(1L, 7L, 1, "A")));
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), qm, mock(WrongQuestionMapper.class), qs);

        PracticeStartVO vo = service.start(7L, startDTO());
        assertThat(vo.getQuestions().get(0).getAnswer()).isNull();
        assertThat(vo.getQuestions().get(0).getAnalysis()).isNull();
    }

    @Test
    void startDefaultNameWhenBlank() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.selectForPractice(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.of(question(1L, 7L, 1, "A")));
        QuestionService qs = mock(QuestionService.class);
        when(qs.toDTO(any(Question.class))).thenReturn(new QuestionDTO());
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), qm, mock(WrongQuestionMapper.class), qs);

        PracticeStartVO vo = service.start(7L, startDTO());
        assertThat(vo.getRecord().getName()).startsWith("顺序练习");
    }

    // ==================== submit ====================

    private PracticeSubmitDTO submitOf(long recordId, Long... questionIds) {
        PracticeSubmitDTO dto = new PracticeSubmitDTO();
        dto.setRecordId(recordId);
        List<PracticeSubmitDTO.AnswerItem> items = new java.util.ArrayList<>();
        for (Long id : questionIds) {
            PracticeSubmitDTO.AnswerItem item = new PracticeSubmitDTO.AnswerItem();
            item.setQuestionId(id);
            item.setAnswer("A");
            items.add(item);
        }
        dto.setAnswers(items);
        return dto;
    }

    @Test
    void submitRecordNotFoundThrows() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(9L)).thenReturn(null);
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), mock(QuestionMapper.class),
                mock(WrongQuestionMapper.class), mock(QuestionService.class));
        assertThatThrownBy(() -> service.submit(7L, submitOf(9L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("练习记录不存在");
    }

    @Test
    void submitNotOwnedThrows() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(9L)).thenReturn(record(9L, 8L, 0, 1, 1));
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), mock(QuestionMapper.class),
                mock(WrongQuestionMapper.class), mock(QuestionService.class));
        assertThatThrownBy(() -> service.submit(7L, submitOf(9L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("练习记录不存在");
    }

    @Test
    void submitAlreadyFinishedThrows() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(9L)).thenReturn(record(9L, 7L, 1, 1, 1));
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), mock(QuestionMapper.class),
                mock(WrongQuestionMapper.class), mock(QuestionService.class));
        assertThatThrownBy(() -> service.submit(7L, submitOf(9L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已提交");
    }

    @Test
    void submitForeignQuestionThrows() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(9L)).thenReturn(record(9L, 7L, 0, 1, 2));
        PracticeQuestionMapper pqm = mock(PracticeQuestionMapper.class);
        when(pqm.selectQuestionIdsByRecord(9L)).thenReturn(List.of(5L));
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                pqm, mock(QuestionMapper.class), mock(WrongQuestionMapper.class),
                mock(QuestionService.class));
        assertThatThrownBy(() -> service.submit(7L, submitOf(9L, 6L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本次练习之外");
    }

    @Test
    void submitExceedsTotalThrows() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(9L)).thenReturn(record(9L, 7L, 0, 1, 1));
        PracticeQuestionMapper pqm = mock(PracticeQuestionMapper.class);
        when(pqm.selectQuestionIdsByRecord(9L)).thenReturn(List.of());
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                pqm, mock(QuestionMapper.class), mock(WrongQuestionMapper.class),
                mock(QuestionService.class));
        assertThatThrownBy(() -> service.submit(7L, submitOf(9L, 1L, 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超过本次练习题目数");
    }

    @Test
    void submitGradesAndWritesWrongBook() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(9L)).thenReturn(record(9L, 7L, 0, 1, 2));
        QuestionMapper qm = mock(QuestionMapper.class);
        // 题10 答错(A vs B), 题11 答对
        PracticeSubmitDTO dto = submitOf(9L, 10L, 11L);
        dto.getAnswers().get(0).setAnswer("B");
        when(qm.selectByIds(List.of(10L, 11L))).thenReturn(
                List.of(question(10L, 7L, 1, "A"), question(11L, 7L, 1, "A")));
        WrongQuestionMapper wm = mock(WrongQuestionMapper.class);
        when(wm.findByUserAndQuestionIds(7L, List.of(10L))).thenReturn(List.of());
        PracticeAnswerMapper am = mock(PracticeAnswerMapper.class);
        PracticeService service = newService(rm, am, mock(PracticeQuestionMapper.class),
                qm, wm, mock(QuestionService.class));

        Map<String, Object> result = service.submit(7L, dto);
        verify(am).insertBatch(anyList());
        verify(wm).insert(any(WrongQuestion.class));
        assertThat(result.get("unanswered")).isEqualTo(0);
        verify(rm).updateFinish(any(PracticeRecord.class));
    }

    @Test
    void submitWrongModeMarksMasteredOnCorrect() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(9L)).thenReturn(record(9L, 7L, 0, 3, 1)); // 错题重做模式
        QuestionMapper qm = mock(QuestionMapper.class);
        when(qm.selectByIds(List.of(10L))).thenReturn(List.of(question(10L, 7L, 1, "A")));
        WrongQuestionMapper wm = mock(WrongQuestionMapper.class);
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), qm, wm, mock(QuestionService.class));

        service.submit(7L, submitOf(9L, 10L));
        verify(wm).updateMastered(7L, 10L, 1);
    }

    @Test
    void submitSkipsForeignQuestions() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(9L)).thenReturn(record(9L, 7L, 0, 1, 1));
        QuestionMapper qm = mock(QuestionMapper.class);
        // selectByIds 返回他人题目 → 判分循环中跳过
        when(qm.selectByIds(List.of(10L))).thenReturn(List.of(question(10L, 8L, 1, "A")));
        PracticeAnswerMapper am = mock(PracticeAnswerMapper.class);
        PracticeService service = newService(rm, am, mock(PracticeQuestionMapper.class),
                qm, mock(WrongQuestionMapper.class), mock(QuestionService.class));

        Map<String, Object> result = service.submit(7L, submitOf(9L, 10L));
        assertThat(result.get("unanswered")).isEqualTo(1); // total 1 - rows 0
        verify(am, never()).insertBatch(anyList());
    }

    // ==================== count / records / detail / delete ====================

    @Test
    void countDelegates() {
        QuestionMapper qm = mock(QuestionMapper.class);
        QuestionService qs = mock(QuestionService.class);
        when(qs.resolvePracticeScope(7L, 5L)).thenReturn(9L);
        when(qm.countForPractice(9L, 2L, 5L, 3, 1, true)).thenReturn(4);
        PracticeService service = newService(mock(PracticeRecordMapper.class),
                mock(PracticeAnswerMapper.class), mock(PracticeQuestionMapper.class),
                qm, mock(WrongQuestionMapper.class), qs);
        assertThat(service.count(7L, 2L, 5L, 3, 1, true)).isEqualTo(4);
    }

    @Test
    void recordsPage() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.selectPage(7L)).thenReturn(List.of(record(1L, 7L, 1, 1, 5)));
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), mock(QuestionMapper.class),
                mock(WrongQuestionMapper.class), mock(QuestionService.class));
        PageInfo<PracticeRecord> page = service.records(7L, 1, 10);
        assertThat(page.getList()).hasSize(1);
    }

    @Test
    void detailOk() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(1L)).thenReturn(record(1L, 7L, 1, 1, 5));
        PracticeAnswerMapper am = mock(PracticeAnswerMapper.class);
        PracticeService service = newService(rm, am, mock(PracticeQuestionMapper.class),
                mock(QuestionMapper.class), mock(WrongQuestionMapper.class), mock(QuestionService.class));
        Map<String, Object> result = service.detail(7L, 1L);
        assertThat(result).containsKey("record");
        verify(am).selectByRecord(1L);
    }

    @Test
    void detailNotOwnedThrows() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(1L)).thenReturn(record(1L, 8L, 1, 1, 5));
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), mock(QuestionMapper.class),
                mock(WrongQuestionMapper.class), mock(QuestionService.class));
        assertThatThrownBy(() -> service.detail(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("练习记录不存在");
    }

    @Test
    void deleteOk() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(1L)).thenReturn(record(1L, 7L, 0, 1, 5));
        PracticeAnswerMapper am = mock(PracticeAnswerMapper.class);
        PracticeService service = newService(rm, am, mock(PracticeQuestionMapper.class),
                mock(QuestionMapper.class), mock(WrongQuestionMapper.class), mock(QuestionService.class));
        service.delete(7L, 1L);
        verify(am).deleteByRecord(1L);
        verify(rm).deleteById(1L);
    }

    @Test
    void deleteMissingThrows() {
        PracticeRecordMapper rm = mock(PracticeRecordMapper.class);
        when(rm.findById(1L)).thenReturn(null);
        PracticeService service = newService(rm, mock(PracticeAnswerMapper.class),
                mock(PracticeQuestionMapper.class), mock(QuestionMapper.class),
                mock(WrongQuestionMapper.class), mock(QuestionService.class));
        assertThatThrownBy(() -> service.delete(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("练习记录不存在");
    }
}
