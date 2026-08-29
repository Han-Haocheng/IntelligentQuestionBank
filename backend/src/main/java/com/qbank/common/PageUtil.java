package com.qbank.common;

/**
 * 分页参数钳制: 防止 pageSize 过大把整表捞进内存
 */
public final class PageUtil {

    public static final int MAX_PAGE_SIZE = 100;

    public static int pageNum(int pageNum) {
        return Math.max(1, pageNum);
    }

    public static int pageSize(int pageSize) {
        return Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
    }

    private PageUtil() {
    }
}
