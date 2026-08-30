package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.dto.ShareDTO;
import com.qbank.entity.Bank;
import com.qbank.entity.Question;
import com.qbank.entity.Share;
import com.qbank.entity.User;
import com.qbank.mapper.BankMapper;
import com.qbank.mapper.QuestionMapper;
import com.qbank.mapper.ShareMapper;
import com.qbank.mapper.ShareMemberMapper;
import com.qbank.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 共享服务测试: 拷贝 / 订阅收件人校验
 */
class ShareServiceTest {

    private ShareService newService(ShareMapper sm, QuestionMapper qm, BankMapper bm,
                                    ShareMemberMapper smm, UserMapper um) {
        return new ShareService(sm, qm, bm, smm, um);
    }

    private Share shareOf(Long id, Long questionId, Long bankId, Long toUserId, int shareType) {
        Share s = new Share();
        s.setId(id);
        s.setQuestionId(questionId);
        s.setBankId(bankId);
        s.setToUserId(toUserId);
        s.setShareType(shareType);
        s.setFromUserId(1L);
        return s;
    }

    @Test
    void copyQuestionDuplicatesWithOrigin() {
        ShareMapper sm = mock(ShareMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        when(sm.findById(10L)).thenReturn(shareOf(10L, 3L, null, 7L, 1));

        Question orig = new Question();
        orig.setId(3L);
        orig.setUserId(1L);
        orig.setTitle("原题");
        orig.setAnswer("A");
        when(qm.findById(3L)).thenReturn(orig);
        when(qm.insert(any(Question.class))).thenAnswer(inv -> {
            inv.getArgument(0, Question.class).setId(999L);
            return 1;
        });

        ShareService service = newService(sm, qm, mock(BankMapper.class), mock(ShareMemberMapper.class),
                mock(UserMapper.class));
        Long newId = service.copy(7L, 10L);
        assertThat(newId).isEqualTo(999L);
        verify(qm).insert(any(Question.class));
    }

    @Test
    void copyBankDuplicatesBankAndQuestions() {
        ShareMapper sm = mock(ShareMapper.class);
        BankMapper bm = mock(BankMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        when(sm.findById(11L)).thenReturn(shareOf(11L, null, 2L, 7L, 3));

        Bank orig = new Bank();
        orig.setId(2L);
        orig.setUserId(1L);
        orig.setName("数据结构");
        when(bm.findById(2L)).thenReturn(orig);
        when(bm.insert(any(Bank.class))).thenAnswer(inv -> {
            inv.getArgument(0, Bank.class).setId(555L);
            return 1;
        });

        Question q1 = new Question();
        q1.setId(5L);
        q1.setTitle("栈");
        Question q2 = new Question();
        q2.setId(6L);
        q2.setTitle("队列");
        when(qm.selectByBank(2L)).thenReturn(List.of(q1, q2));

        ShareService service = newService(sm, qm, bm, mock(ShareMemberMapper.class), mock(UserMapper.class));
        Long newId = service.copy(7L, 11L);
        assertThat(newId).isEqualTo(555L);
        verify(bm).insert(any(Bank.class));
        verify(qm, org.mockito.Mockito.times(2)).insert(any(Question.class));
    }

    @Test
    void copyRejectedForNonRecipient() {
        ShareMapper sm = mock(ShareMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        // 该共享收件人是 8, 调用者 7
        when(sm.findById(10L)).thenReturn(shareOf(10L, 3L, null, 8L, 1));
        ShareService service = newService(sm, qm, mock(BankMapper.class), mock(ShareMemberMapper.class),
                mock(UserMapper.class));
        assertThatThrownBy(() -> service.copy(7L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
        verify(qm, never()).insert(any());
    }

    @Test
    void subscribeRejectedForNonRecipient() {
        ShareMapper sm = mock(ShareMapper.class);
        ShareMemberMapper smm = mock(ShareMemberMapper.class);
        when(sm.findById(10L)).thenReturn(shareOf(10L, 3L, null, 8L, 1));
        ShareService service = newService(sm, mock(QuestionMapper.class), mock(BankMapper.class),
                smm, mock(UserMapper.class));
        assertThatThrownBy(() -> service.subscribe(7L, 10L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
        verify(smm, never()).upsert(anyLong(), anyLong(), anyInt());
    }

    @Test
    void publicShareVisibleToAnyoneButOwner() {
        ShareMapper sm = mock(ShareMapper.class);
        // 公开题目共享(shareType 2, 无 toUserId), 共享者 1
        when(sm.findById(12L)).thenReturn(shareOf(12L, 4L, null, null, 2));
        ShareService service = newService(sm, mock(QuestionMapper.class), mock(BankMapper.class),
                mock(ShareMemberMapper.class), mock(UserMapper.class));
        // 共享者本人不能拷贝自己的公开共享
        assertThatThrownBy(() -> service.copy(1L, 12L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    // ==================== share 补充 ====================

    @Test
    void shareUserQuestionOk() {
        ShareMapper sm = mock(ShareMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        Question q = new Question();
        q.setId(3L);
        q.setUserId(1L);
        when(qm.findById(3L)).thenReturn(q);
        User target = new User();
        target.setId(8L);
        UserMapper um = mock(UserMapper.class);
        when(um.findByUsername("bob")).thenReturn(target);
        ShareMemberMapper smm = mock(ShareMemberMapper.class);
        ShareService service = newService(sm, qm, mock(BankMapper.class), smm, um);

        ShareDTO dto = new ShareDTO();
        dto.setQuestionId(3L);
        dto.setToUsername("bob");
        service.share(1L, dto);
        verify(sm).insert(any(Share.class));
        verify(smm).upsert(any(), eq(8L), eq(1));
    }

    @Test
    void shareQuestionMissingTargetUserThrows() {
        ShareMapper sm = mock(ShareMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        Question q = new Question();
        q.setId(3L);
        q.setUserId(1L);
        when(qm.findById(3L)).thenReturn(q);
        UserMapper um = mock(UserMapper.class);
        when(um.findByUsername("nobody")).thenReturn(null);
        ShareService service = newService(sm, qm, mock(BankMapper.class),
                mock(ShareMemberMapper.class), um);
        ShareDTO dto = new ShareDTO();
        dto.setQuestionId(3L);
        dto.setToUsername("nobody");
        assertThatThrownBy(() -> service.share(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
        verify(sm, never()).insert(any(Share.class));
    }

    @Test
    void shareQuestionToSelfThrows() {
        QuestionMapper qm = mock(QuestionMapper.class);
        Question q = new Question();
        q.setId(3L);
        q.setUserId(1L);
        when(qm.findById(3L)).thenReturn(q);
        User target = new User();
        target.setId(1L);
        UserMapper um = mock(UserMapper.class);
        when(um.findByUsername("me")).thenReturn(target);
        ShareService service = newService(mock(ShareMapper.class), qm, mock(BankMapper.class),
                mock(ShareMemberMapper.class), um);
        ShareDTO dto = new ShareDTO();
        dto.setQuestionId(3L);
        dto.setToUsername("me");
        assertThatThrownBy(() -> service.share(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能共享给自己");
    }

    @Test
    void shareQuestionPublicAlreadyPublicThrows() {
        ShareMapper sm = mock(ShareMapper.class);
        QuestionMapper qm = mock(QuestionMapper.class);
        Question q = new Question();
        q.setId(3L);
        q.setUserId(1L);
        when(qm.findById(3L)).thenReturn(q);
        when(sm.insertPublic(any(Share.class))).thenReturn(0);
        ShareService service = newService(sm, qm, mock(BankMapper.class),
                mock(ShareMemberMapper.class), mock(UserMapper.class));
        ShareDTO dto = new ShareDTO();
        dto.setQuestionId(3L);
        dto.setShareType(2);
        assertThatThrownBy(() -> service.share(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已公开共享");
    }

    @Test
    void shareBankNotOwnedThrows() {
        BankMapper bm = mock(BankMapper.class);
        Bank bank = new Bank();
        bank.setId(2L);
        bank.setUserId(9L);
        when(bm.findById(2L)).thenReturn(bank);
        ShareService service = newService(mock(ShareMapper.class), mock(QuestionMapper.class),
                bm, mock(ShareMemberMapper.class), mock(UserMapper.class));
        ShareDTO dto = new ShareDTO();
        dto.setBankId(2L);
        assertThatThrownBy(() -> service.share(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能共享自己的题库");
    }

    @Test
    void updatePermissionInvalidThrows() {
        ShareService service = newService(mock(ShareMapper.class), mock(QuestionMapper.class),
                mock(BankMapper.class), mock(ShareMemberMapper.class), mock(UserMapper.class));
        assertThatThrownBy(() -> service.updatePermission(1L, 1L, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限不合法");
    }

    @Test
    void updatePermissionNotOwnerThrows() {
        ShareMapper sm = mock(ShareMapper.class);
        when(sm.findById(1L)).thenReturn(shareOf(1L, 3L, null, 8L, 1));
        ShareService service = newService(sm, mock(QuestionMapper.class), mock(BankMapper.class),
                mock(ShareMemberMapper.class), mock(UserMapper.class));
        assertThatThrownBy(() -> service.updatePermission(9L, 1L, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    @Test
    void updatePermissionPublicFixedReadThrows() {
        ShareMapper sm = mock(ShareMapper.class);
        when(sm.findById(1L)).thenReturn(shareOf(1L, 3L, null, null, 2));
        ShareService service = newService(sm, mock(QuestionMapper.class), mock(BankMapper.class),
                mock(ShareMemberMapper.class), mock(UserMapper.class));
        assertThatThrownBy(() -> service.updatePermission(1L, 1L, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("公开共享固定只读");
    }

    @Test
    void updatePermissionOk() {
        ShareMapper sm = mock(ShareMapper.class);
        when(sm.findById(1L)).thenReturn(shareOf(1L, 3L, null, 8L, 1));
        ShareService service = newService(sm, mock(QuestionMapper.class), mock(BankMapper.class),
                mock(ShareMemberMapper.class), mock(UserMapper.class));
        service.updatePermission(1L, 1L, 2);
        verify(sm).updatePermission(1L, 2);
    }

    @Test
    void cancelNotOwnerThrows() {
        ShareMapper sm = mock(ShareMapper.class);
        when(sm.delete(1L, 9L)).thenReturn(0);
        ShareService service = newService(sm, mock(QuestionMapper.class), mock(BankMapper.class),
                mock(ShareMemberMapper.class), mock(UserMapper.class));
        assertThatThrownBy(() -> service.cancel(9L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权取消");
    }

    @Test
    void cancelOk() {
        ShareMapper sm = mock(ShareMapper.class);
        when(sm.delete(1L, 1L)).thenReturn(1);
        ShareService service = newService(sm, mock(QuestionMapper.class), mock(BankMapper.class),
                mock(ShareMemberMapper.class), mock(UserMapper.class));
        service.cancel(1L, 1L);
        verify(sm).delete(1L, 1L);
    }

    @Test
    void sentDelegates() {
        ShareMapper sm = mock(ShareMapper.class);
        when(sm.selectSent(1L)).thenReturn(List.of(shareOf(1L, 3L, null, 8L, 1)));
        ShareService service = newService(sm, mock(QuestionMapper.class), mock(BankMapper.class),
                mock(ShareMemberMapper.class), mock(UserMapper.class));
        PageInfo<Share> page = service.sent(1L, 1, 10);
        assertThat(page.getList()).hasSize(1);
        PageHelper.clearPage();
    }

    @Test
    void receivedDelegates() {
        ShareMapper sm = mock(ShareMapper.class);
        when(sm.selectReceived(8L)).thenReturn(List.of(shareOf(1L, 3L, null, 8L, 1)));
        ShareService service = newService(sm, mock(QuestionMapper.class), mock(BankMapper.class),
                mock(ShareMemberMapper.class), mock(UserMapper.class));
        PageInfo<Share> page = service.received(8L, 1, 10);
        assertThat(page.getList()).hasSize(1);
        PageHelper.clearPage();
    }
}
