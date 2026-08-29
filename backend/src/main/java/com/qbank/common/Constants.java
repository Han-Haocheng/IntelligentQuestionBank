package com.qbank.common;

import java.util.Arrays;
import java.util.List;

/**
 * 常量定义
 */
public class Constants {

    /** 题型: 1单选 2多选 3填空 4判断 5简答 */
    public static final int TYPE_SINGLE = 1;
    public static final int TYPE_MULTIPLE = 2;
    public static final int TYPE_FILL = 3;
    public static final int TYPE_JUDGE = 4;
    public static final int TYPE_SHORT = 5;

    public static final List<String> TYPE_NAMES = Arrays.asList("单选题", "多选题", "填空题", "判断题", "简答题");

    /** 难度名称(下标+1=难度值) */
    public static final List<String> DIFFICULTY_NAMES = Arrays.asList("入门", "简单", "中等", "较难", "困难");

    public static final int ROLE_ADMIN = 0;
    public static final int ROLE_USER = 1;

    /** 共享类型: 1指定用户-题目 2公开-题目 3指定用户-题库 4公开-题库 */
    public static final int SHARE_TYPE_USER_QUESTION = 1;
    public static final int SHARE_TYPE_PUBLIC_QUESTION = 2;
    public static final int SHARE_TYPE_USER_BANK = 3;
    public static final int SHARE_TYPE_PUBLIC_BANK = 4;

    /** 共享权限: 1只读 2可编辑 */
    public static final int PERMISSION_READ = 1;
    public static final int PERMISSION_EDIT = 2;

    /** 练习模式: 1顺序 2随机 3错题重做 */
    public static final int PRACTICE_MODE_SEQUENCE = 1;
    public static final int PRACTICE_MODE_RANDOM = 2;
    public static final int PRACTICE_MODE_WRONG = 3;

    /** 练习状态: 0进行中 1已完成 */
    public static final int PRACTICE_STATUS_STARTED = 0;
    public static final int PRACTICE_STATUS_FINISHED = 1;

    public static final int TOKEN_DAYS = 7;

    public static String typeName(int type) {
        return (type >= 1 && type <= 5) ? TYPE_NAMES.get(type - 1) : "未知";
    }

    public static String difficultyName(int difficulty) {
        return (difficulty >= 1 && difficulty <= 5) ? DIFFICULTY_NAMES.get(difficulty - 1) : "未知";
    }

    private Constants() {
    }
}
