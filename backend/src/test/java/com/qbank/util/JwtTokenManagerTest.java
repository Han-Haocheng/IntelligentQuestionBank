package com.qbank.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 令牌测试
 */
class JwtTokenManagerTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef0123456789abcdef";

    private final JwtTokenManager manager = new JwtTokenManager(SECRET);

    @Test
    void createAndVerify() {
        String token = manager.create(42L, 0);
        JwtTokenManager.TokenInfo info = manager.verify(token);
        assertThat(info).isNotNull();
        assertThat(info.userId).isEqualTo(42L);
        assertThat(info.role).isEqualTo(0);
        assertThat(info.expireAt).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void rejectsNullAndBlank() {
        assertThat(manager.verify(null)).isNull();
        assertThat(manager.verify("")).isNull();
    }

    @Test
    void rejectsTamperedToken() {
        String token = manager.create(1L, 1);
        // 修改签名段末字符(真实改动签名内容)
        String tampered = token.substring(0, token.length() - 1) + "a";
        assertThat(manager.verify(tampered)).isNull();
        // 修改 payload 段(篡改内容)
        String[] parts = token.split("\\.");
        String payloadTampered = parts[0] + "." + "eyJzdWIiOiI5OTkiLCJyb2xlIjoxfQ" + "." + parts[2];
        assertThat(manager.verify(payloadTampered)).isNull();
    }

    @Test
    void rejectsGarbage() {
        assertThat(manager.verify("not-a-jwt")).isNull();
        assertThat(manager.verify("a.b.c")).isNull();
    }

    @Test
    void rejectsTokenSignedByAnotherKey() {
        JwtTokenManager other = new JwtTokenManager("another-secret-0123456789abcdef0123456789abcdef");
        String token = other.create(7L, 1);
        assertThat(manager.verify(token)).isNull();
    }
}
