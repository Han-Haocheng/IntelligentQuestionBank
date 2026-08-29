package com.qbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qbank.common.BusinessException;
import com.qbank.dto.ImportRowDTO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 批量导入解析/校验测试(parse 路径不依赖 mapper)
 */
class QuestionImportServiceTest {

    private static final String HEADER =
            "题干,题型,选项A,选项B,选项C,选项D,选项E,选项F,答案,解析,难度,知识点标签,来源";

    private final QuestionImportService service =
            new QuestionImportService(null, null, null, new ObjectMapper());

    private List<ImportRowDTO> parseCsv(String body) throws Exception {
        String csv = HEADER + "\n" + body;
        return service.parse("test.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parseCsvRecognizesTypesAndAnswers() throws Exception {
        List<ImportRowDTO> rows = parseCsv(
                "测试题,单选题,A,B,C,D,,,A,解析,1,标签,自编\n"
                + "判断一题,判断题,,,,,,,对,解析,2,标签,自编\n"
                + "多选一题,多选题,A,B,C,,, ,AB,解析,3,标签,自编\n");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getType()).isEqualTo(1);
        assertThat(rows.get(1).getType()).isEqualTo(4);
        assertThat(rows.get(1).getAnswer()).isEqualTo("对");
        assertThat(rows.get(2).getType()).isEqualTo(2);
        assertThat(rows.get(2).getAnswer()).isEqualTo("AB");
        assertThat(rows.get(0).getErrors()).isEmpty();
    }

    @Test
    void parseCsvCollectsRowErrors() throws Exception {
        List<ImportRowDTO> rows = parseCsv(",单选题,A,B,,,,,A,,1,标签,自编\n");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getErrors()).contains("题干不能为空");
    }

    @Test
    void parseRejectsWrongHeader() {
        String csv = "第一列,第二列,,,,,,,,,,,\nA,B,,,,,,,,,,,\n";
        assertThatThrownBy(() -> service.parse("t.csv",
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("表头不符合模板");
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThatThrownBy(() -> service.parse("t.txt",
                new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }
}
