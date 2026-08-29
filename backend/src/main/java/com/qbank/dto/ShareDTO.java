package com.qbank.dto;

public class ShareDTO {
    private Long questionId;
    private Long bankId;        // 题库共享时使用
    private String toUsername;
    private Integer shareType;  // 1指定用户-题目 2公开-题目 3指定用户-题库 4公开-题库
    private Integer permission; // 1只读(默认) 2可编辑; 公开共享固定只读
    private String message;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }
    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }
    public Integer getShareType() { return shareType; }
    public void setShareType(Integer shareType) { this.shareType = shareType; }
    public Integer getPermission() { return permission; }
    public void setPermission(Integer permission) { this.permission = permission; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
