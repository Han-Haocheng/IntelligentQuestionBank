package com.qbank.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理(服务端渲染版)
 * 业务异常/系统异常统一渲染 error 页面(带提示信息), 不再返回 JSON;
 * 页面内可选的 fetch 异步请求请自行 try/catch 处理错误提示
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, Model model, HttpServletRequest request) {
        log.warn("business error: {}", e.getMessage());
        model.addAttribute("message", e.getMessage());
        model.addAttribute("status", 400);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception e, Model model) {
        // 完整堆栈仅记录在服务端日志, 不向页面透出内部细节
        log.error("system error", e);
        model.addAttribute("message", "系统繁忙, 请稍后重试");
        model.addAttribute("status", 500);
        return "error";
    }
}
