package com.qbank.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 密码工具: 存储格式为 盐:sha256(盐+原文)
 */
public class PasswordUtil {

    public static String encode(String raw) {
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return salt + ":" + sha256(salt + raw);
    }

    public static boolean matches(String raw, String stored) {
        if (raw == null || raw.isEmpty() || stored == null || !stored.contains(":")) {
            return false;
        }
        int idx = stored.indexOf(':');
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
