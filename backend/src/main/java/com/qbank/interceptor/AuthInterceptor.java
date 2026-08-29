package com.qbank.interceptor;

import com.qbank.util.JwtTokenManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器: 仅校验 Authorization 头(Bearer token)
 * 不再支持 URL ?token= 传参, 避免令牌泄露进访问日志/浏览器历史
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenManager tokenManager;

    public AuthInterceptor(JwtTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        JwtTokenManager.TokenInfo info = tokenManager.verify(token);
        if (info == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已过期\",\"data\":null}");
            return false;
        }
        request.setAttribute("userId", info.userId);
        request.setAttribute("role", info.role);
        request.setAttribute("token", token);
        return true;
    }
}
