package com.qbank.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录/注册限流测试
 */
class LoginRateLimiterTest {

    @Test
    void locksAfterMaxFailures() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        assertThat(limiter.isLoginLocked("alice")).isFalse();
        for (int i = 0; i < LoginRateLimiter.MAX_LOGIN_FAILURES; i++) {
            limiter.onLoginFailure("alice");
        }
        assertThat(limiter.isLoginLocked("alice")).isTrue();
    }

    @Test
    void successResetsCounter() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < LoginRateLimiter.MAX_LOGIN_FAILURES - 1; i++) {
            limiter.onLoginFailure("bob");
        }
        assertThat(limiter.isLoginLocked("bob")).isFalse();
        limiter.onLoginSuccess("bob");
        limiter.onLoginFailure("bob");
        assertThat(limiter.isLoginLocked("bob")).isFalse();
    }

    @Test
    void failuresArePerUser() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < LoginRateLimiter.MAX_LOGIN_FAILURES; i++) {
            limiter.onLoginFailure("carol");
        }
        assertThat(limiter.isLoginLocked("carol")).isTrue();
        assertThat(limiter.isLoginLocked("dave")).isFalse();
    }

    @Test
    void registerLimitedAfterWindowMax() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < LoginRateLimiter.MAX_REGISTER_PER_WINDOW; i++) {
            assertThat(limiter.tryRegister()).isTrue();
        }
        assertThat(limiter.tryRegister()).isFalse();
    }

    @Test
    void nullOrEmptyUsernameNoop() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        assertThat(limiter.isLoginLocked("")).isFalse();
        assertThat(limiter.isLoginLocked(null)).isFalse();
        limiter.onLoginFailure("");
        limiter.onLoginFailure(null);
        limiter.onLoginSuccess("");
        limiter.onLoginSuccess(null);
        // 空输入不应污染其他用户
        assertThat(limiter.isLoginLocked("x")).isFalse();
    }
}
