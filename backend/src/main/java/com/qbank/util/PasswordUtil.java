package com.qbank.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码工具
 * 新存储格式: BCrypt 哈希(自适应加盐, 抗 GPU 爆破)
 * 兼容旧格式: 盐:sha256(盐+原文) —— 仅用于存量账号校验, 登录成功后自动升级为 BCrypt
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public static String encode(String raw) {
        return ENCODER.encode(raw);
    }

    public static boolean matches(String raw, String stored) {
        if (raw == null || raw.isEmpty() || stored == null || stored.isEmpty()) {
            return false;
        }
        if (isLegacySha256(stored)) {
            return legacyMatches(raw, stored);
        }
        try {
            return ENCODER.matches(raw, stored);
        } catch (IllegalArgumentException e) {
            // 非法存储串(如损坏的哈希), 一律视为不匹配
            return false;
        }
    }

    /** 旧版存储格式为 "盐:sha256(盐+原文)"(含冒号); BCrypt 哈希以 $2 开头且不含冒号 */
    public static boolean isLegacySha256(String stored) {
        return stored != null && stored.indexOf(':') >= 0;
    }

    private static boolean legacyMatches(String raw, String stored) {
        int idx = stored.indexOf(':');
        if (idx <= 0 || idx == stored.length() - 1) {
            return false;
        }
        String salt = stored.substring(0, idx);
        String hash = stored.substring(idx + 1);
        return sha256(salt + raw).equals(hash);
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private PasswordUtil() {
    }
}
