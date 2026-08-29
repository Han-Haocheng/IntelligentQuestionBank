package com.qbank.dto;

public class ShareDTO {
    private Long questionId;
    private String toUsername;
    private Integer shareType;  // 1指定用户 2公开
    private String message;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }
    public Integer getShareType() { return shareType; }
    public void setShareType(Integer shareType) { this.shareType = shareType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
