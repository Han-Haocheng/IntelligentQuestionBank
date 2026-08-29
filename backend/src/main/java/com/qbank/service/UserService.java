package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.PageUtil;
import com.qbank.dto.LoginDTO;
import com.qbank.dto.LoginVO;
import com.qbank.dto.RegisterDTO;
import com.qbank.dto.UserUpdateDTO;
import com.qbank.entity.User;
import com.qbank.mapper.UserMapper;
import com.qbank.util.JwtTokenManager;
import com.qbank.util.LoginRateLimiter;
import com.qbank.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtTokenManager tokenManager;
    private final LoginRateLimiter loginRateLimiter;

    public UserService(UserMapper userMapper, JwtTokenManager tokenManager, LoginRateLimiter loginRateLimiter) {
        this.userMapper = userMapper;
        this.tokenManager = tokenManager;
        this.loginRateLimiter = loginRateLimiter;
    }

    public LoginVO register(RegisterDTO dto) {
        checkUsernamePassword(dto);
        if (!loginRateLimiter.tryRegister()) {
            throw new BusinessException("注册过于频繁, 请稍后再试");
        }
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRole(1);
        user.setStatus(1);
        userMapper.insert(user);
        return buildLogin(user);
    }

    public LoginVO login(LoginDTO dto) {
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("请输入用户名和密码");
        }
        String username = dto.getUsername().trim();
        if (loginRateLimiter.isLoginLocked(username)) {
            throw new BusinessException("尝试次数过多, 请稍后再试");
        }
        User user = userMapper.findByUsername(username);
        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            loginRateLimiter.onLoginFailure(username);
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用, 请联系管理员");
        }
        loginRateLimiter.onLoginSuccess(username);
        // 存量旧格式(盐:sha256)账号登录成功后自动升级为 BCrypt, 无需用户感知
        if (PasswordUtil.isLegacySha256(user.getPassword())) {
            userMapper.updatePassword(user.getId(), PasswordUtil.encode(dto.getPassword()));
        }
        return buildLogin(user);
    }

    public void logout(String token) {
        tokenManager.remove(token);
    }

    public User info(Long userId) {
        return requireUser(userId);
    }

    public User update(Long userId, UserUpdateDTO dto) {
        User user = requireUser(userId);
        if (StringUtils.hasText(dto.getNewPassword())) {
            if (!PasswordUtil.matches(dto.getOldPassword(), user.getPassword())) {
                throw new BusinessException("原密码错误");
            }
            if (dto.getNewPassword().length() < 6) {
                throw new BusinessException("新密码长度不能少于6位");
            }
            userMapper.updatePassword(userId, PasswordUtil.encode(dto.getNewPassword()));
        }
        User update = new User();
        update.setId(userId);
        update.setNickname(dto.getNickname());
        update.setEmail(dto.getEmail());
        update.setAvatar(dto.getAvatar());
        userMapper.update(update);
        return requireUser(userId);
    }

    public PageInfo<User> page(String keyword, int pageNum, int pageSize) {
        PageHelper.startPage(PageUtil.pageNum(pageNum), PageUtil.pageSize(pageSize));
        return new PageInfo<>(userMapper.selectPage(keyword));
    }

    public User add(RegisterDTO dto) {
        checkUsernamePassword(dto);
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRole(dto.getRole() != null && dto.getRole() == 0 ? 0 : 1);
        user.setStatus(1);
        userMapper.insert(user);
        return requireUser(user.getId());
    }

    public void updateStatus(Long operatorId, Long id, Integer status) {
        checkAdminTarget(operatorId, id);
        userMapper.updateStatus(id, status != null && status == 1 ? 1 : 0);
    }

    public void resetPassword(Long operatorId, Long id) {
        checkAdminTarget(operatorId, id);
        userMapper.updatePassword(id, PasswordUtil.encode("123456"));
    }

    public void delete(Long operatorId, Long id) {
        checkAdminTarget(operatorId, id);
        userMapper.deleteById(id);
    }

    private LoginVO buildLogin(User user) {
        user.setPassword(null);
        String token = tokenManager.create(user.getId(), user.getRole());
        return new LoginVO(token, user);
    }

    private User requireUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private void checkAdminTarget(Long operatorId, Long targetId) {
        if (operatorId.equals(targetId)) {
            throw new BusinessException("不能对当前登录账号执行该操作");
        }
        User target = requireUser(targetId);
        if (target.getRole() != null && target.getRole() == 0) {
            throw new BusinessException("不能对管理员账号执行该操作");
        }
    }

    private void checkUsernamePassword(RegisterDTO dto) {
        if (!StringUtils.hasText(dto.getUsername()) || dto.getUsername().trim().length() < 3) {
            throw new BusinessException("用户名至少3个字符");
        }
        if (!StringUtils.hasText(dto.getPassword()) || dto.getPassword().length() < 6) {
            throw new BusinessException("密码至少6位");
        }
    }
}
