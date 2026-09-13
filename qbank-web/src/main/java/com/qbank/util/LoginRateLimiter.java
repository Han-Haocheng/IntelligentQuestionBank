package com.qbank.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录/注册限流(内存版, 单机够用)
 * - 登录: 同 IP+用户名 连续失败 5 次锁定 15 分钟, 登录成功即清零
 * - 注册: 同 IP 每小时最多 10 次, 超出后限频 1 小时
 * - 达到阈值时惰性清理过期条目, 避免内存无限增长
 * - 安全(issue #8): 用户名校验 key 归一为小写防大小写变体绕过;
 *   X-Forwarded-For 默认不信任(仅直连 remoteAddr), 部署在代理后需显式开启
 *   qbank-web.rate-limit.trust-x-forwarded-for=true; 锁定到期惰性清零计数器
 */
@Component
public class LoginRateLimiter {

    public static final int MAX_LOGIN_FAILURES = 5;
    public static final long LOGIN_LOCK_MILLIS = 15L * 60 * 1000;   // 15 分钟
    public static final int MAX_REGISTER_PER_WINDOW = 10;
    public static final long REGISTER_WINDOW_MILLIS = 60L * 60 * 1000; // 1 小时
    public static final long REGISTER_BLOCK_MILLIS = 60L * 60 * 1000;  // 1 小时

    private static final int MAX_ENTRIES = 5000;

    /** 是否信任 X-Forwarded-For 头(仅部署在可信反向代理后时开启, 默认关闭防伪造) */
    @Value("${qbank-web.rate-limit.trust-x-forwarded-for:false}")
    private boolean trustXForwardedFor;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public static final class Entry {
        int failures;
        long lockUntil;
        int regCount;
        long regWindowStart;
    }

    // ==================== 登录 ====================

    public boolean isLoginLocked(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Entry e = store.get(loginKey(username));
        if (e == null) {
            return false;
        }
        if (e.lockUntil > now) {
            return true;
        }
        // 锁定到期惰性清零, 避免解锁后计数器残留导致立即可再锁 (issue #8)
        if (e.failures > 0 || e.lockUntil > 0) {
            e.failures = 0;
            e.lockUntil = 0;
        }
        return false;
    }

    public void onLoginFailure(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }
        String key = loginKey(username);
        long now = System.currentTimeMillis();
        Entry e = store.computeIfAbsent(key, k -> new Entry());
        e.failures++;
        if (e.failures >= MAX_LOGIN_FAILURES) {
            e.lockUntil = now + LOGIN_LOCK_MILLIS;
        }
        maybeClean(now);
    }

    public void onLoginSuccess(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }
        store.remove(loginKey(username));
    }

    // ==================== 注册 ====================

    /** 允许注册则计数并返回 true; 超限返回 false */
    public boolean tryRegister() {
        long now = System.currentTimeMillis();
        String key = "reg|" + clientIp();
        Entry e = store.computeIfAbsent(key, k -> {
            Entry n = new Entry();
            n.regWindowStart = now;
            return n;
        });
        if (now - e.regWindowStart >= REGISTER_WINDOW_MILLIS) {
            // 窗口过期, 重置
            e.regWindowStart = now;
            e.regCount = 0;
        }
        if (e.regCount >= MAX_REGISTER_PER_WINDOW) {
            return false;
        }
        e.regCount++;
        maybeClean(now);
        return true;
    }

    // ==================== 内部 ====================

    /** 用户名校验 key 归一为小写, 防止大小写变体绕过锁定 (issue #8) */
    private String loginKey(String username) {
        return "login|" + clientIp() + "|" + username.toLowerCase(Locale.ROOT);
    }

    private void maybeClean(long now) {
        if (store.size() > MAX_ENTRIES) {
            store.entrySet().removeIf(en -> {
                Entry e = en.getValue();
                return e.lockUntil > 0 && e.lockUntil < now
                        && now - e.regWindowStart >= REGISTER_WINDOW_MILLIS;
            });
        }
    }

    /**
     * 客户端真实 IP: 默认只取直连地址(攻击者无法伪造);
     * 仅当显式配置 trust-x-forwarded-for=true(可信反代后) 才取 XFF 首项
     */
    private String clientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                if (trustXForwardedFor) {
                    String forwarded = req.getHeader("X-Forwarded-For");
                    if (forwarded != null && !forwarded.isBlank()) {
                        return forwarded.split(",")[0].trim();
                    }
                }
                String ip = req.getRemoteAddr();
                return ip == null ? "unknown" : ip;
            }
        } catch (Exception ignored) {
            // 无请求上下文(如单测)时归为 unknown
        }
        return "unknown";
    }
}
