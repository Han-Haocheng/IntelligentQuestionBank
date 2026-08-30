package com.qbank.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 常量工具测试: 题型/难度名称映射边界
 */
class ConstantsTest {

    @Test
    void typeNameBoundaries() {
        assertThat(Constants.typeName(1)).isEqualTo("单选题");
        assertThat(Constants.typeName(3)).isEqualTo("填空题");
        assertThat(Constants.typeName(5)).isEqualTo("简答题");
        assertThat(Constants.typeName(0)).isEqualTo("未知");
        assertThat(Constants.typeName(6)).isEqualTo("未知");
        assertThat(Constants.TYPE_NAMES).hasSize(5);
    }

    @Test
    void difficultyNameBoundaries() {
        assertThat(Constants.difficultyName(1)).isEqualTo("入门");
        assertThat(Constants.difficultyName(5)).isEqualTo("困难");
        assertThat(Constants.difficultyName(0)).isEqualTo("未知");
        assertThat(Constants.difficultyName(6)).isEqualTo("未知");
        assertThat(Constants.DIFFICULTY_NAMES).hasSize(5);
    }
}
