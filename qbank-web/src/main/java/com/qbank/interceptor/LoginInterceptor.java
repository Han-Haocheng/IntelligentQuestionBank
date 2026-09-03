package com.qbank.interceptor;

import com.qbank.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器(服务端渲染版, 基于 Session)
 * 未登录访问受保护页面时重定向到登录页
 */
public class LoginInterceptor implements HandlerInterceptor {

    /** Session 中保存登录用户的对象名 */
    public static final String SESSION_USER = "qbankLoginUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_USER) instanceof User) {
            return true;
        }
        // 部分接口由 fetch 异步调用时, 返回 401 便于前端识别; 页面请求整页跳转登录页
        if (isAsyncRequest(request)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
        return false;
    }

    public static boolean isAsyncRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if (requestedWith != null && !requestedWith.isBlank()) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }
}
