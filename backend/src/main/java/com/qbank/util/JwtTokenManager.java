package com.qbank.util;

import com.qbank.common.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 令牌管理器 (HMAC-SHA256 签名) + 服务端会话白名单
 * 已签发 token 记录在内存白名单中: 登出(remove)立即失效;
 * 后端重启后白名单清空, 所有已签发 token 一并失效, 用户需重新登录
 * 密钥来自配置 qbank.jwt.secret, 生产环境务必通过环境变量注入高强度随机串
 */
@Component
public class JwtTokenManager {

    public static class TokenInfo {
        public Long userId;
        public Integer role;
        public long expireAt;
    }

    private final SecretKey key;

    /** 已签发 token 白名单: token -> 过期时间戳(毫秒); 进程重启即清空, 全部会话失效 */
    private final Map<String, Long> validTokens = new ConcurrentHashMap<>();

    public JwtTokenManager(@Value("${qbank.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String create(Long userId, Integer role) {
        long now = System.currentTimeMillis();
        long exp = now + Constants.TOKEN_DAYS * 24L * 3600 * 1000;
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key)
                .compact();
        validTokens.put(token, exp);
        purgeExpired();
        return token;
    }

    public TokenInfo verify(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        // 服务端白名单: 未签发/已登出/后端重启后签发记录丢失的 token 一律无效
        Long expireAt = validTokens.get(token);
        if (expireAt == null) {
            return null;
        }
        if (expireAt < System.currentTimeMillis()) {
            validTokens.remove(token);
            return null;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            TokenInfo info = new TokenInfo();
            info.userId = Long.valueOf(claims.getSubject());
            Object role = claims.get("role");
            info.role = role instanceof Integer ? (Integer) role : Integer.valueOf(String.valueOf(role));
            info.expireAt = claims.getExpiration().getTime();
            return info;
        } catch (Exception e) {
            // 签名不符/过期/格式非法, 一律视为无效并移出白名单
            validTokens.remove(token);
            return null;
        }
    }

    /** 登出: 从白名单移除, token 立即失效 */
    public void remove(String token) {
        if (token != null && !token.isEmpty()) {
            validTokens.remove(token);
        }
    }

    /** 惰性清理过期条目, 防止白名单无限增长 */
    private void purgeExpired() {
        long now = System.currentTimeMillis();
        validTokens.entrySet().removeIf(e -> e.getValue() < now);
    }
}