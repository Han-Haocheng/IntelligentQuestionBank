package com.qbank.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 密码工具测试: BCrypt 往返 + 旧格式兼容
 */
class PasswordUtilTest {

    @Test
    void bcryptRoundTrip() {
        String hash = PasswordUtil.encode("abc123");
        assertThat(hash).startsWith("$2");
        assertThat(PasswordUtil.isLegacySha256(hash)).isFalse();
        assertThat(PasswordUtil.matches("abc123", hash)).isTrue();
        assertThat(PasswordUtil.matches("wrong-pass", hash)).isFalse();
    }

    @Test
    void bcryptHashesAreSaltedAndDiffer() {
        assertThat(PasswordUtil.encode("same")).isNotEqualTo(PasswordUtil.encode("same"));
    }

    @Test
    void legacySha256StillVerifies() {
        // init.sql 中 admin 账号的旧格式哈希(密码 123456)
        String stored = "f70037850279020b:a525c44d6f86180f2d8a620f42990f263dca54b57af1a6a4d9ea8fbaff4595e4";
        assertThat(PasswordUtil.isLegacySha256(stored)).isTrue();
        assertThat(PasswordUtil.matches("123456", stored)).isTrue();
        assertThat(PasswordUtil.matches("wrong", stored)).isFalse();
    }

    @Test
    void rejectsBlankOrBrokenInput() {
        assertThat(PasswordUtil.matches("", "whatever")).isFalse();
        assertThat(PasswordUtil.matches(null, "whatever")).isFalse();
        assertThat(PasswordUtil.matches("x", "")).isFalse();
        assertThat(PasswordUtil.matches("x", "broken-hash")).isFalse();
        assertThat(PasswordUtil.matches("x", ":no-salt")).isFalse();
    }
}
