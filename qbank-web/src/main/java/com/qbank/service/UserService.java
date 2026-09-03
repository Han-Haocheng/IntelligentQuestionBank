package com.qbank.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qbank.common.BusinessException;
import com.qbank.common.Constants;
import com.qbank.common.PageUtil;
import com.qbank.dto.LoginDTO;
import com.qbank.dto.RegisterDTO;
import com.qbank.dto.UserUpdateDTO;
import com.qbank.entity.User;
import com.qbank.mapper.UserMapper;
import com.qbank.util.LoginRateLimiter;
import com.qbank.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务(服务端渲染版)
 * 登录态由 Web 层 Session 维护, 此处仅完成校验/注册/资料维护, 不再签发 JWT
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final LoginRateLimiter loginRateLimiter;

    public UserService(UserMapper userMapper, LoginRateLimiter loginRateLimiter) {
        this.userMapper = userMapper;
        this.loginRateLimiter = loginRateLimiter;
    }

    public User register(RegisterDTO dto) {
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
        user.setRole(Constants.ROLE_USER);
        user.setStatus(1);
        userMapper.insert(user);
        return requireUser(user.getId());
    }

    /** 登录成功返回用户(密码已清空), 由调用方写入 Session; 失败抛出业务异常 */
    public User login(LoginDTO dto) {
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
        user.setPassword(null);
        return user;
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
        user.setRole(dto.getRole() != null && dto.getRole() == Constants.ROLE_ADMIN
                ? Constants.ROLE_ADMIN : Constants.ROLE_USER);
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

    private User requireUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
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
