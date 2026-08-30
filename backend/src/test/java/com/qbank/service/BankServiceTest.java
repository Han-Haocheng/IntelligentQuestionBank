package com.qbank.service;

import com.qbank.common.BusinessException;
import com.qbank.entity.Bank;
import com.qbank.mapper.BankMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.ShareMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题库服务测试: 列表合并 / 重名校验 / 归属校验 / 删除清理
 */
class BankServiceTest {

    private BankService newService(BankMapper bm, QuestionMapper qm, ShareMapper sm) {
        return new BankService(bm, qm, sm);
    }

    private Bank bank(long id, long ownerId, String name) {
        Bank b = new Bank();
        b.setId(id);
        b.setUserId(ownerId);
        b.setName(name);
        return b;
    }

    @Test
    void listAsAdminReturnsAllUsers() {
        BankMapper bankMapper = mock(BankMapper.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(bankMapper.selectByUser(null)).thenReturn(List.of(bank(1L, 1L, "a"), bank(2L, 2L, "b")));
        when(questionMapper.countByBank(1L)).thenReturn(3);
        when(questionMapper.countByBank(2L)).thenReturn(5);

        BankService service = newService(bankMapper, questionMapper, mock(ShareMapper.class));
        List<Bank> list = service.list(1L, 0);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getQuestionCount()).isEqualTo(3);
        assertThat(list.get(1).getQuestionCount()).isEqualTo(5);
        verify(bankMapper).selectByUser(null);
    }

    @Test
    void listAsUserMergesSharedDeduplicated() {
        BankMapper bankMapper = mock(BankMapper.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(bankMapper.selectByUser(7L)).thenReturn(List.of(bank(1L, 7L, "own")));
        // 共享列表中含与自有重复的 1 号题库
        when(bankMapper.selectShared(7L)).thenReturn(List.of(bank(1L, 7L, "own"), bank(2L, 9L, "shared")));
        when(questionMapper.countByBank(anyLong())).thenReturn(1);

        BankService service = newService(bankMapper, questionMapper, mock(ShareMapper.class));
        List<Bank> list = service.list(7L, 1);

        assertThat(list).extracting(Bank::getId).containsExactly(1L, 2L);
    }

    @Test
    void getOwnedReturns() {
        BankMapper bankMapper = mock(BankMapper.class);
        when(bankMapper.findById(1L)).thenReturn(bank(1L, 7L, "x"));
        BankService service = newService(bankMapper, mock(QuestionMapper.class), mock(ShareMapper.class));
        assertThat(service.get(7L, 1L).getName()).isEqualTo("x");
    }

    @Test
    void getForeignOrMissingThrows() {
        BankMapper bankMapper = mock(BankMapper.class);
        when(bankMapper.findById(1L)).thenReturn(bank(1L, 9L, "other"));
        BankService service = newService(bankMapper, mock(QuestionMapper.class), mock(ShareMapper.class));
        assertThatThrownBy(() -> service.get(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");

        when(bankMapper.findById(2L)).thenReturn(null);
        assertThatThrownBy(() -> service.get(7L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    @Test
    void addValidSetsOwnerAndInserts() {
        BankMapper bankMapper = mock(BankMapper.class);
        BankService service = newService(bankMapper, mock(QuestionMapper.class), mock(ShareMapper.class));
        Bank b = bank(0, 0, "Java基础");
        service.add(7L, b);
        assertThat(b.getUserId()).isEqualTo(7L);
        verify(bankMapper).insert(b);
    }

    @Test
    void addDuplicateNameThrows() {
        BankMapper bankMapper = mock(BankMapper.class);
        when(bankMapper.insert(any(Bank.class))).thenThrow(new DuplicateKeyException("dup"));
        BankService service = newService(bankMapper, mock(QuestionMapper.class), mock(ShareMapper.class));
        Bank b = bank(0, 0, "Java基础");
        assertThatThrownBy(() -> service.add(7L, b))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在同名题库");
    }

    @Test
    void addBlankNameThrows() {
        BankMapper bankMapper = mock(BankMapper.class);
        BankService service = newService(bankMapper, mock(QuestionMapper.class), mock(ShareMapper.class));
        Bank b = bank(0, 0, "   ");
        assertThatThrownBy(() -> service.add(7L, b))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("题库名称不能为空");
        verify(bankMapper, never()).insert(any());
    }

    @Test
    void addTooLongNameThrows() {
        BankService service = newService(mock(BankMapper.class), mock(QuestionMapper.class), mock(ShareMapper.class));
        Bank b = bank(0, 0, "x".repeat(51));
        assertThatThrownBy(() -> service.add(7L, b))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过50个字符");
    }

    @Test
    void addTooLongDescriptionThrows() {
        BankService service = newService(mock(BankMapper.class), mock(QuestionMapper.class), mock(ShareMapper.class));
        Bank b = bank(0, 0, "合法名称");
        b.setDescription("y".repeat(501));
        assertThatThrownBy(() -> service.add(7L, b))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过500个字符");
    }

    @Test
    void updateMissingIdThrows() {
        BankService service = newService(mock(BankMapper.class), mock(QuestionMapper.class), mock(ShareMapper.class));
        Bank b = new Bank();
        b.setName("x");  // id 为 null
        assertThatThrownBy(() -> service.update(7L, b))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("题库ID不能为空");
    }

    @Test
    void updateDuplicateNameOtherThrows() {
        BankMapper bankMapper = mock(BankMapper.class);
        when(bankMapper.findById(1L)).thenReturn(bank(1L, 7L, "x"));
        when(bankMapper.findByName(7L, "重复名")).thenReturn(bank(2L, 7L, "重复名"));
        BankService service = newService(bankMapper, mock(QuestionMapper.class), mock(ShareMapper.class));
        Bank b = bank(1L, 7L, "重复名");
        assertThatThrownBy(() -> service.update(7L, b))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在同名题库");
        verify(bankMapper, never()).update(any());
    }

    @Test
    void updateSameNameSelfAllowed() {
        BankMapper bankMapper = mock(BankMapper.class);
        when(bankMapper.findById(1L)).thenReturn(bank(1L, 7L, "x"));
        when(bankMapper.findByName(7L, "x")).thenReturn(bank(1L, 7L, "x"));
        BankService service = newService(bankMapper, mock(QuestionMapper.class), mock(ShareMapper.class));
        Bank b = bank(1L, 7L, "x");
        service.update(7L, b);
        verify(bankMapper).update(b);
    }

    @Test
    void deleteClearsBankSharesAndDeletes() {
        BankMapper bankMapper = mock(BankMapper.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        ShareMapper shareMapper = mock(ShareMapper.class);
        when(bankMapper.findById(1L)).thenReturn(bank(1L, 7L, "x"));
        BankService service = newService(bankMapper, questionMapper, shareMapper);
        service.delete(7L, 1L);
        verify(questionMapper).clearBank(1L);
        verify(shareMapper).deleteByBank(1L);
        verify(bankMapper).deleteById(1L);
    }

    @Test
    void deleteNotOwnedThrows() {
        BankMapper bankMapper = mock(BankMapper.class);
        when(bankMapper.findById(1L)).thenReturn(bank(1L, 9L, "other"));
        BankService service = newService(bankMapper, mock(QuestionMapper.class), mock(ShareMapper.class));
        assertThatThrownBy(() -> service.delete(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
        verify(bankMapper, never()).deleteById(eq(1L));
    }
}
