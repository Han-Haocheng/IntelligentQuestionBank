package com.qbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.dto.QuestionDTO;
import com.qbank.dto.QuestionQuery;
import com.qbank.entity.Bank;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题目服务测试: 分类归属 / 共享可编辑
 */
class QuestionServiceTest {

    private QuestionService newService(QuestionMapper qm, CategoryMapper cm, BankMapper bm,
                                       FavoriteMapper fm, ShareMapper sm, WrongQuestionMapper wm) {
        return new QuestionService(qm, cm, bm, fm, sm, wm, new ObjectMapper());
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

    private QuestionDTO updateDTO(long id, String title) {
        QuestionDTO dto = validDTO();
        dto.setId(id);
        dto.setTitle(title);
        return dto;
    }

    private Question existing(long id, long ownerId, Long bankId) {
        Question q = new Question();
        q.setId(id);
        q.setUserId(ownerId);
        q.setCategoryId(1L);
        q.setBankId(bankId);
        return q;
    }

    @Test
    void addRejectsMissingCategory() {
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        when(categoryMapper.findById(1L)).thenReturn(null);  // 分类不存在
        QuestionService service = newService(mock(QuestionMapper.class), categoryMapper,
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        assertThatThrownBy(() -> service.add(7L, validDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void addAcceptsAnyExistingCategory() {
        // v1.1 分类全局共享: 他人分类也可选用, 仅校验存在
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        Category foreign = new Category();
        foreign.setId(1L);
        foreign.setUserId(99L);
        when(categoryMapper.findById(1L)).thenReturn(foreign);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionService service = newService(questionMapper, categoryMapper, mock(BankMapper.class),
                mock(FavoriteMapper.class), mock(ShareMapper.class), mock(WrongQuestionMapper.class));
        service.add(7L, validDTO());
        verify(questionMapper).insert(any(Question.class));
    }

    @Test
    void updateAllowedForSharedEditorPreservesOwnershipFields() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        ShareMapper shareMapper = mock(ShareMapper.class);
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        BankMapper bankMapper = mock(BankMapper.class);
        when(questionMapper.findById(3L)).thenReturn(existing(3L, 99L, 2L));
        when(shareMapper.countEditable(eq(3L), eq(7L))).thenReturn(1);
        // 原分类/题库存在且归属题主
        Category cat = new Category();
        cat.setId(1L);
        cat.setUserId(99L);
        when(categoryMapper.findById(1L)).thenReturn(cat);
        Bank bank = new Bank();
        bank.setId(2L);
        bank.setUserId(99L);
        when(bankMapper.findById(2L)).thenReturn(bank);

        QuestionService service = newService(questionMapper, categoryMapper, bankMapper,
                mock(FavoriteMapper.class), shareMapper, mock(WrongQuestionMapper.class));
        QuestionDTO dto = updateDTO(3L, "改标题");
        dto.setCategoryId(999L);   // 编辑者试图改分类 → 应被强制保留
        dto.setBankId(888L);       // 编辑者试图改题库 → 应被强制保留
        service.update(7L, 1, dto);

        // 归属字段被还原为原值
        assertThat(dto.getCategoryId()).isEqualTo(1L);
        assertThat(dto.getBankId()).isEqualTo(2L);
        verify(questionMapper).update(any(Question.class));
    }

    @Test
    void updateRejectedWithoutEditPermission() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        ShareMapper shareMapper = mock(ShareMapper.class);
        when(questionMapper.findById(3L)).thenReturn(existing(3L, 99L, null));
        when(shareMapper.countEditable(eq(3L), eq(7L))).thenReturn(0);

        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), shareMapper,
                mock(WrongQuestionMapper.class));
        assertThatThrownBy(() -> service.update(7L, 1, updateDTO(3L, "x")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权修改");
        verify(questionMapper, never()).update(any());
    }

    @Test
    void addToSharedEditableBankOwnsToBankOwner() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        BankMapper bankMapper = mock(BankMapper.class);
        ShareMapper shareMapper = mock(ShareMapper.class);
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        when(categoryMapper.findById(1L)).thenReturn(null);  // 无分类

        Bank shared = new Bank();
        shared.setId(2L);
        shared.setUserId(99L);
        when(bankMapper.findById(2L)).thenReturn(shared);
        when(shareMapper.countBankEditable(eq(2L), eq(7L))).thenReturn(1);

        QuestionService service = newService(questionMapper, categoryMapper, bankMapper,
                mock(FavoriteMapper.class), shareMapper, mock(WrongQuestionMapper.class));
        QuestionDTO dto = validDTO();
        dto.setCategoryId(null);  // 无分类, 只测共享题库加入
        dto.setBankId(2L);
        service.add(7L, dto);
        verify(questionMapper).insert(any(Question.class));
    }

    // ==================== page / get 补充 ====================

    @Test
    void pageAdminUsesQueryUserId() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionQuery query = new QuestionQuery();
        query.setUserId(5L);
        when(questionMapper.selectPage(5L, query)).thenReturn(List.of());
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        PageInfo<QuestionDTO> page = service.page(1L, 0, query);
        assertThat(page.getList()).isEmpty();
        verify(questionMapper).selectPage(5L, query);
        PageHelper.clearPage();
    }

    @Test
    void pageNormalUserUsesOwnScope() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionQuery query = new QuestionQuery();
        when(questionMapper.selectPage(7L, query)).thenReturn(List.of());
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        PageInfo<QuestionDTO> page = service.page(7L, 1, query);
        assertThat(page.getList()).isEmpty();
        verify(questionMapper).selectPage(7L, query);
        PageHelper.clearPage();
    }

    @Test
    void pageForeignBankWithoutAccessThrows() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        BankMapper bankMapper = mock(BankMapper.class);
        ShareMapper shareMapper = mock(ShareMapper.class);
        Bank foreign = new Bank();
        foreign.setId(2L);
        foreign.setUserId(99L);
        when(bankMapper.findById(2L)).thenReturn(foreign);
        when(shareMapper.countBankAccessible(2L, 7L)).thenReturn(0);
        QuestionQuery query = new QuestionQuery();
        query.setBankId(2L);
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                bankMapper, mock(FavoriteMapper.class), shareMapper, mock(WrongQuestionMapper.class));
        assertThatThrownBy(() -> service.page(7L, 1, query))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权查看该题库");
        PageHelper.clearPage();
    }

    @Test
    void getForeignPrivateThrows() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(questionMapper.findById(3L)).thenReturn(existing(3L, 99L, null));
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        assertThatThrownBy(() -> service.get(7L, 1, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权查看该题目");
    }

    @Test
    void getSharedAccessibleExposesOwner() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        ShareMapper shareMapper = mock(ShareMapper.class);
        when(questionMapper.findById(3L)).thenReturn(existing(3L, 99L, null));
        when(shareMapper.countAccessible(3L, 7L)).thenReturn(1);
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), shareMapper,
                mock(WrongQuestionMapper.class));
        QuestionDTO dto = service.get(7L, 1, 3L);
        assertThat(dto.getUserId()).isEqualTo(99L);
    }

    @Test
    void getMissingThrows() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(questionMapper.findById(3L)).thenReturn(null);
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        assertThatThrownBy(() -> service.get(7L, 1, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("题目不存在");
    }

    // ==================== add / delete 补充 ====================

    @Test
    void addEmptyTitleThrows() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        QuestionDTO dto = validDTO();
        dto.setTitle("  ");
        assertThatThrownBy(() -> service.add(7L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("题干不能为空");
        verify(questionMapper, never()).insert(any());
    }

    @Test
    void addSingleChoiceAnswerOutOfRangeThrows() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        QuestionDTO dto = validDTO();
        dto.setAnswer("C");  // 仅 A/B 两个选项
        assertThatThrownBy(() -> service.add(7L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("答案必须是选项中的一个字母");
        verify(questionMapper, never()).insert(any());
    }

    @Test
    void addMultipleChoiceAnswerOutOfRangeThrows() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        QuestionDTO dto = validDTO();
        dto.setType(2);
        dto.setAnswer("AC");  // C 超出 A/B 范围
        assertThatThrownBy(() -> service.add(7L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超出选项范围");
        verify(questionMapper, never()).insert(any());
    }

    @Test
    void deleteEmptyIdsThrows() {
        QuestionService service = newService(mock(QuestionMapper.class), mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        assertThatThrownBy(() -> service.delete(7L, 1, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请选择要删除的题目");
    }

    @Test
    void deleteForeignNotAdminThrows() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(questionMapper.findById(1L)).thenReturn(existing(1L, 99L, null));
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), mock(FavoriteMapper.class), mock(ShareMapper.class),
                mock(WrongQuestionMapper.class));
        assertThatThrownBy(() -> service.delete(7L, 1, List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权删除题目");
        verify(questionMapper, never()).deleteByIds(any());
    }

    @Test
    void deleteOkCleansUp() {
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        FavoriteMapper favoriteMapper = mock(FavoriteMapper.class);
        ShareMapper shareMapper = mock(ShareMapper.class);
        WrongQuestionMapper wrongQuestionMapper = mock(WrongQuestionMapper.class);
        when(questionMapper.findById(1L)).thenReturn(existing(1L, 7L, null));
        when(questionMapper.findById(2L)).thenReturn(null);  // 不存在的题目跳过
        QuestionService service = newService(questionMapper, mock(CategoryMapper.class),
                mock(BankMapper.class), favoriteMapper, shareMapper, wrongQuestionMapper);
        service.delete(7L, 1, List.of(1L, 2L));
        verify(questionMapper).deleteByIds(List.of(1L, 2L));
        verify(favoriteMapper).deleteByQuestionIds(List.of(1L, 2L));
        verify(shareMapper).deleteByQuestionIds(List.of(1L, 2L));
        verify(wrongQuestionMapper).deleteByQuestionIds(List.of(1L, 2L));
    }
}
