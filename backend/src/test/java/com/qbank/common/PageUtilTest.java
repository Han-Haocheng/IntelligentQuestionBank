package com.qbank.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页参数钳制测试
 */
class PageUtilTest {

    @Test
    void pageNumClampedToMinOne() {
        assertThat(PageUtil.pageNum(0)).isEqualTo(1);
        assertThat(PageUtil.pageNum(-5)).isEqualTo(1);
        assertThat(PageUtil.pageNum(3)).isEqualTo(3);
    }

    @Test
    void pageSizeClampedToMaxOneHundred() {
        assertThat(PageUtil.pageSize(0)).isEqualTo(1);
        assertThat(PageUtil.pageSize(-1)).isEqualTo(1);
        assertThat(PageUtil.pageSize(200)).isEqualTo(100);
        assertThat(PageUtil.pageSize(20)).isEqualTo(20);
    }
}
