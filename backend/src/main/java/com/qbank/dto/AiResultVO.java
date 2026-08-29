package com.qbank.dto;

/**
 * AI 分析结果
 */
public class AiResultVO {
    private String content;
    private String model;    // 模型名或 local(本地规则)

    public AiResultVO() {
    }

    public AiResultVO(String content, String model) {
        this.content = content;
        this.model = model;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
