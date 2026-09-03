package com.qbank.common;

/**
 * 业务异常: 消息直接透出给前端
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
