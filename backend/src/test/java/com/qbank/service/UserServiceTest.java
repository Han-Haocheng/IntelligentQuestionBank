package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.dto.LoginDTO;
import com.qbank.dto.LoginVO;
import com.qbank.dto.RegisterDTO;
import com.qbank.dto.UserUpdateDTO;
import com.qbank.entity.User;
import com.qbank.mapper.UserMapper;
import com.qbank.util.JwtTokenManager;
import com.qbank.util.LoginRateLimiter;
import com.qbank.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务测试: 注册/登录/改密/管理员操作保护
 */
class UserServiceTest {

    @AfterEach
    void clearPageHelper() {
        PageHelper.clearPage();
    }

    private UserService newService(UserMapper um, JwtTokenManager tm, LoginRateLimiter lr) {
        return new UserService(um, tm, lr);
    }

    private RegisterDTO registerDTO(String username, String password) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    private LoginDTO loginDTO(String username, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    private User user(long id, String username, String password, int role, int status) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPassword(password);
        u.setRole(role);
        u.setStatus(status);
        return u;
    }

    // ==================== register ====================

    @Test
    void registerShortUsernameThrows() {
        UserMapper um = mock(UserMapper.class);
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        assertThatThrownBy(() -> service.register(registerDTO("ab", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名至少3个字符");
        verify(um, never()).insert(any());
    }

    @Test
    void registerShortPasswordThrows() {
        UserService service = newService(mock(UserMapper.class), mock(JwtTokenManager.class),
                mock(LoginRateLimiter.class));
        assertThatThrownBy(() -> service.register(registerDTO("alice", "123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码至少6位");
    }

    @Test
    void registerRateLimitedThrows() {
        LoginRateLimiter lr = mock(LoginRateLimiter.class);
        when(lr.tryRegister()).thenReturn(false);
        UserService service = newService(mock(UserMapper.class), mock(JwtTokenManager.class), lr);
        assertThatThrownBy(() -> service.register(registerDTO("alice", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("注册过于频繁");
    }

    @Test
    void registerDuplicateUsernameThrows() {
        UserMapper um = mock(UserMapper.class);
        when(um.findByUsername("alice")).thenReturn(new User());
        LoginRateLimiter lr = mock(LoginRateLimiter.class);
        when(lr.tryRegister()).thenReturn(true);
        UserService service = newService(um, mock(JwtTokenManager.class), lr);
        assertThatThrownBy(() -> service.register(registerDTO("alice", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void registerOk() {
        UserMapper um = mock(UserMapper.class);
        when(um.insert(any(User.class))).thenAnswer(inv -> {
            inv.getArgument(0, User.class).setId(100L);
            return 1;
        });
        JwtTokenManager tm = mock(JwtTokenManager.class);
        when(tm.create(100L, 1)).thenReturn("token-abc");
        LoginRateLimiter lr = mock(LoginRateLimiter.class);
        when(lr.tryRegister()).thenReturn(true);
        UserService service = newService(um, tm, lr);

        RegisterDTO dto = registerDTO("alice", "123456");
        LoginVO vo = service.register(dto);
        assertThat(vo.getToken()).isEqualTo("token-abc");
        assertThat(vo.getUser().getNickname()).isEqualTo("alice");
        assertThat(vo.getUser().getRole()).isEqualTo(1);
        assertThat(vo.getUser().getStatus()).isEqualTo(1);
        assertThat(vo.getUser().getPassword()).isNull();
        verify(um).insert(any(User.class));
    }

    // ==================== login ====================

    @Test
    void loginMissingCredentialsThrows() {
        UserService service = newService(mock(UserMapper.class), mock(JwtTokenManager.class),
                mock(LoginRateLimiter.class));
        assertThatThrownBy(() -> service.login(loginDTO("", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请输入用户名和密码");
        assertThatThrownBy(() -> service.login(loginDTO("alice", "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请输入用户名和密码");
    }

    @Test
    void loginLockedThrows() {
        LoginRateLimiter lr = mock(LoginRateLimiter.class);
        when(lr.isLoginLocked("alice")).thenReturn(true);
        UserService service = newService(mock(UserMapper.class), mock(JwtTokenManager.class), lr);
        assertThatThrownBy(() -> service.login(loginDTO("alice", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尝试次数过多");
    }

    @Test
    void loginWrongPasswordThrows() {
        UserMapper um = mock(UserMapper.class);
        when(um.findByUsername("alice")).thenReturn(user(1L, "alice", PasswordUtil.encode("secret"), 1, 1));
        LoginRateLimiter lr = mock(LoginRateLimiter.class);
        UserService service = newService(um, mock(JwtTokenManager.class), lr);
        assertThatThrownBy(() -> service.login(loginDTO("alice", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
        verify(lr).onLoginFailure("alice");
    }

    @Test
    void loginUserNotFoundThrows() {
        UserMapper um = mock(UserMapper.class);
        when(um.findByUsername("nobody")).thenReturn(null);
        LoginRateLimiter lr = mock(LoginRateLimiter.class);
        UserService service = newService(um, mock(JwtTokenManager.class), lr);
        assertThatThrownBy(() -> service.login(loginDTO("nobody", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void loginDisabledThrows() {
        UserMapper um = mock(UserMapper.class);
        when(um.findByUsername("alice")).thenReturn(user(1L, "alice", PasswordUtil.encode("secret"), 1, 0));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        assertThatThrownBy(() -> service.login(loginDTO("alice", "secret")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁用");
    }

    @Test
    void loginLegacySha256UpgradesPassword() {
        UserMapper um = mock(UserMapper.class);
        // init.sql 旧格式哈希(密码 123456)
        String legacy = "f70037850279020b:a525c44d6f86180f2d8a620f42990f263dca54b57af1a6a4d9ea8fbaff4595e4";
        when(um.findByUsername("alice")).thenReturn(user(1L, "alice", legacy, 1, 1));
        JwtTokenManager tm = mock(JwtTokenManager.class);
        when(tm.create(1L, 1)).thenReturn("t");
        UserService service = newService(um, tm, mock(LoginRateLimiter.class));
        LoginVO vo = service.login(loginDTO("alice", "123456"));
        assertThat(vo.getToken()).isEqualTo("t");
        verify(um).updatePassword(eq(1L), anyString());
    }

    @Test
    void loginOk() {
        UserMapper um = mock(UserMapper.class);
        when(um.findByUsername("alice")).thenReturn(user(1L, "alice", PasswordUtil.encode("secret"), 1, 1));
        JwtTokenManager tm = mock(JwtTokenManager.class);
        when(tm.create(1L, 1)).thenReturn("token");
        LoginRateLimiter lr = mock(LoginRateLimiter.class);
        UserService service = newService(um, tm, lr);
        LoginVO vo = service.login(loginDTO("alice", "secret"));
        assertThat(vo.getToken()).isEqualTo("token");
        verify(lr).onLoginSuccess("alice");
        verify(um, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void logoutRemovesToken() {
        JwtTokenManager tm = mock(JwtTokenManager.class);
        UserService service = newService(mock(UserMapper.class), tm, mock(LoginRateLimiter.class));
        service.logout("token-x");
        verify(tm).remove("token-x");
    }

    // ==================== info / update ====================

    @Test
    void infoMissingThrows() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(1L)).thenReturn(null);
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        assertThatThrownBy(() -> service.info(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void infoOk() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(1L)).thenReturn(user(1L, "alice", "x", 1, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        assertThat(service.info(1L).getUsername()).isEqualTo("alice");
    }

    @Test
    void updateWrongOldPasswordThrows() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(1L)).thenReturn(user(1L, "alice", PasswordUtil.encode("oldpass"), 1, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setOldPassword("wrong");
        dto.setNewPassword("newpass123");
        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("原密码错误");
    }

    @Test
    void updateNewPasswordTooShortThrows() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(1L)).thenReturn(user(1L, "alice", PasswordUtil.encode("oldpass"), 1, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setOldPassword("oldpass");
        dto.setNewPassword("123");
        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("少于6位");
    }

    @Test
    void updateOk() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(1L)).thenReturn(user(1L, "alice", PasswordUtil.encode("oldpass"), 1, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNickname("新昵称");
        dto.setEmail("a@b.com");
        dto.setOldPassword("oldpass");
        dto.setNewPassword("newpass123");
        service.update(1L, dto);
        verify(um).updatePassword(eq(1L), anyString());
        verify(um).update(any(User.class));
    }

    // ==================== page / add ====================

    @Test
    void pageDelegates() {
        UserMapper um = mock(UserMapper.class);
        when(um.selectPage("al")).thenReturn(List.of(user(1L, "alice", null, 1, 1)));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        PageInfo<User> page = service.page("al", 1, 10);
        assertThat(page.getList()).hasSize(1);
        verify(um).selectPage("al");
    }

    @Test
    void addRespectsAdminRole() {
        UserMapper um = mock(UserMapper.class);
        when(um.insert(any(User.class))).thenAnswer(inv -> {
            inv.getArgument(0, User.class).setId(9L);
            return 1;
        });
        when(um.findById(9L)).thenReturn(user(9L, "admin2", null, 0, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        RegisterDTO dto = registerDTO("admin2", "123456");
        dto.setRole(0);
        User created = service.add(dto);
        assertThat(created.getRole()).isEqualTo(0);
    }

    @Test
    void addDefaultsUserRole() {
        UserMapper um = mock(UserMapper.class);
        when(um.insert(any(User.class))).thenAnswer(inv -> {
            inv.getArgument(0, User.class).setId(9L);
            return 1;
        });
        when(um.findById(9L)).thenReturn(user(9L, "user2", null, 1, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        User created = service.add(registerDTO("user2", "123456"));
        assertThat(created.getRole()).isEqualTo(1);
    }

    // ==================== 管理员操作 ====================

    @Test
    void updateStatusSelfThrows() {
        UserService service = newService(mock(UserMapper.class), mock(JwtTokenManager.class),
                mock(LoginRateLimiter.class));
        assertThatThrownBy(() -> service.updateStatus(7L, 7L, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能对当前登录账号");
    }

    @Test
    void updateStatusAdminTargetThrows() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(2L)).thenReturn(user(2L, "boss", null, 0, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        assertThatThrownBy(() -> service.updateStatus(7L, 2L, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能对管理员");
    }

    @Test
    void updateStatusOk() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(2L)).thenReturn(user(2L, "user2", null, 1, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        service.updateStatus(7L, 2L, 1);
        verify(um).updateStatus(2L, 1);
        service.updateStatus(7L, 2L, null);
        verify(um).updateStatus(2L, 0);
    }

    @Test
    void resetPasswordOk() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(2L)).thenReturn(user(2L, "user2", null, 1, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        service.resetPassword(7L, 2L);
        verify(um).updatePassword(eq(2L), anyString());
    }

    @Test
    void deleteOk() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(2L)).thenReturn(user(2L, "user2", null, 1, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        service.delete(7L, 2L);
        verify(um).deleteById(2L);
    }

    @Test
    void deleteAdminTargetThrows() {
        UserMapper um = mock(UserMapper.class);
        when(um.findById(2L)).thenReturn(user(2L, "boss", null, 0, 1));
        UserService service = newService(um, mock(JwtTokenManager.class), mock(LoginRateLimiter.class));
        assertThatThrownBy(() -> service.delete(7L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能对管理员");
        verify(um, never()).deleteById(anyLong());
    }
}
