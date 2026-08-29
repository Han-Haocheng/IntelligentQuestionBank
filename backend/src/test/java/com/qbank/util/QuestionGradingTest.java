package com.qbank.util;

import com.qbank.entity.Question;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 练习判分规则测试
 */
class QuestionGradingTest {

    private Question question(int type, String answer) {
        Question q = new Question();
        q.setType(type);
        q.setAnswer(answer);
        return q;
    }

    @Test
    void singleChoiceIgnoreCase() {
        assertThat(QuestionGrading.isCorrect(question(1, "B"), "b")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(1, "B"), "A")).isFalse();
    }

    @Test
    void multipleChoiceOrderIndependent() {
        assertThat(QuestionGrading.isCorrect(question(2, "ABD"), "DAB")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(2, "ABD"), "A B D")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(2, "ABD"), "ABC")).isFalse();
    }

    @Test
    void fillBlankMultiBlank() {
        assertThat(QuestionGrading.isCorrect(question(3, "final|||extends"), "final|||extends")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(3, "final|||extends"), "Final|||Extends")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(3, "final|||extends"), "final|extends")).isFalse();
        assertThat(QuestionGrading.isCorrect(question(3, "final|||extends"), "final|||extends|||x")).isFalse();
    }

    @Test
    void judgeNormalization() {
        assertThat(QuestionGrading.isCorrect(question(4, "对"), "对")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(4, "对"), "true")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(4, "对"), "T")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(4, "错"), "错")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(4, "错"), "其他内容")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(4, "错"), "对")).isFalse();
    }

    @Test
    void shortAnswerLenient() {
        assertThat(QuestionGrading.isCorrect(question(5, "栈是后进先出"), "栈是后进先出")).isTrue();
        assertThat(QuestionGrading.isCorrect(question(5, "ABC"), "abc")).isTrue();
    }

    @Test
    void blankOrEmptyNeverCorrect() {
        assertThat(QuestionGrading.isCorrect(question(1, "A"), "")).isFalse();
        assertThat(QuestionGrading.isCorrect(question(1, "A"), null)).isFalse();
        assertThat(QuestionGrading.isCorrect(question(1, ""), "A")).isFalse();
    }
}
