package com.qbank.dto;

/**
 * 练习趋势项
 */
public class TrendVO {
    private String date;
    private Long total;
    private Long correct;
    private Double accuracy;   // 百分比数值

    public TrendVO() {
    }

    public TrendVO(String date, Long total, Long correct, Double accuracy) {
        this.date = date;
        this.total = total;
        this.correct = correct;
        this.accuracy = accuracy;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Long getCorrect() { return correct; }
    public void setCorrect(Long correct) { this.correct = correct; }
    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
}
