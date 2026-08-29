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

/**
 * JWT 无状态令牌管理器 (HMAC-SHA256 签名)
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

    public JwtTokenManager(@Value("${qbank.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String create(Long userId, Integer role) {
        long now = System.currentTimeMillis();
        long exp = now + Constants.TOKEN_DAYS * 24L * 3600 * 1000;
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key)
                .compact();
    }

    public TokenInfo verify(String token) {
        if (token == null || token.isEmpty()) {
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
            // 签名不符/过期/格式非法, 一律视为无效
            return null;
        }
    }

    /**
     * JWT 无状态, 无需服务端删除; 如需立即失效可后续引入黑名单或缩短 TTL
     */
    public void remove(String token) {
        // no-op
    }
}
