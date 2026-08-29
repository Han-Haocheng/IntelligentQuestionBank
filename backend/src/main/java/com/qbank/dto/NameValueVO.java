package com.qbank.dto;

/**
 * 统计图通用项: 名称-数量
 */
public class NameValueVO {
    private String name;
    private Long value;

    public NameValueVO() {
    }

    public NameValueVO(String name, Long value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
}
