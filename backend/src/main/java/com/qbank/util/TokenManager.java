package com.qbank.util;

import com.qbank.common.Constants;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 Token 管理器(单机版, 重启后失效)
 */
@Component
public class TokenManager {

    public static class TokenInfo {
        public Long userId;
        public Integer role;
        public long expireAt;
    }

    private final Map<String, TokenInfo> store = new ConcurrentHashMap<>();

    public String create(Long userId, Integer role) {
        String token = UUID.randomUUID().toString().replace("-", "");
        TokenInfo info = new TokenInfo();
        info.userId = userId;
        info.role = role;
        info.expireAt = System.currentTimeMillis() + Constants.TOKEN_DAYS * 24L * 3600 * 1000;
        store.put(token, info);
        cleanIfNeeded();
        return token;
    }

    public TokenInfo verify(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        TokenInfo info = store.get(token);
        if (info == null) {
            return null;
        }
        if (info.expireAt < System.currentTimeMillis()) {
            store.remove(token);
            return null;
        }
        return info;
    }

    public void remove(String token) {
        if (token != null) {
            store.remove(token);
        }
    }

    private void cleanIfNeeded() {
        if (store.size() > 10000) {
            long now = System.currentTimeMillis();
            store.entrySet().removeIf(e -> e.getValue().expireAt < now);
        }
    }
}
