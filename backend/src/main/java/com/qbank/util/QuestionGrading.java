package com.qbank.util;

import com.qbank.common.Constants;
import com.qbank.entity.Question;

import java.util.ArrayList;
import java.util.List;

/**
 * 练习判分纯逻辑(无状态, 便于单测)
 * 判分规则:
 * - 单选/简答: 忽略大小写宽松相等
 * - 多选: 字母归一化后排序比较(与选项顺序无关)
 * - 填空: 多空用 ||| 分隔, 逐空忽略大小写比较
 * - 判断: 对/正确/true/T 归一为"对", 其余视为"错"
 */
public final class QuestionGrading {

    public static boolean isCorrect(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }
        String answer = question.getAnswer() == null ? "" : question.getAnswer().trim();
        if (answer.isEmpty()) {
            return false;
        }
        String ua = userAnswer.trim();
        switch (question.getType() == null ? 0 : question.getType()) {
            case Constants.TYPE_MULTIPLE: {
                return normalizeLetters(ua).equals(normalizeLetters(answer));
            }
            case Constants.TYPE_FILL: {
                String[] expect = splitMultiBlank(answer);
                String[] actual = splitMultiBlank(ua);
                if (expect.length != actual.length) {
                    return false;
                }
                for (int i = 0; i < expect.length; i++) {
                    if (!expect[i].trim().equalsIgnoreCase(actual[i].trim())) {
                        return false;
                    }
                }
                return true;
            }
            case Constants.TYPE_JUDGE: {
                return normalizeJudge(ua).equals(normalizeJudge(answer));
            }
            default:
                // 单选/简答: 宽松相等比较
                return ua.equalsIgnoreCase(answer);
        }
    }

    /** 按字面量 '|||' 拆分多空答案(不使用正则) */
    public static String[] splitMultiBlank(String s) {
        List<String> parts = new ArrayList<>();
        String sep = "|||";
        int start = 0;
        int idx;
        while ((idx = s.indexOf(sep, start)) >= 0) {
            parts.add(s.substring(start, idx));
            start = idx + sep.length();
        }
        parts.add(s.substring(start));
        return parts.toArray(new String[0]);
    }

    public static String normalizeLetters(String s) {
        char[] chars = s.toUpperCase().replaceAll("[^A-Z]", "").toCharArray();
        java.util.Arrays.sort(chars);
        return new String(chars);
    }

    public static String normalizeJudge(String s) {
        String v = s.trim();
        if (v.equals("对") || v.equals("正确") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("T")) {
            return "对";
        }
        return "错";
    }

    private QuestionGrading() {
    }
}
