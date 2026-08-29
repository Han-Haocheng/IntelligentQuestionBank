package com.qbank.entity;

import java.time.LocalDateTime;

/**
 * 共享
 */
public class Share {

    private Long id;
    private Long questionId;
    private Long bankId;         // 题库共享时使用
    private Long fromUserId;
    private Long toUserId;
    private Integer shareType;   // 1指定用户 2公开
    private Integer permission;  // 1只读 2可编辑(公开共享固定只读)
    private String message;
    private LocalDateTime createTime;

    /** 非数据库字段 */
    private String questionTitle;
    private String bankName;
    private String fromUsername;
    private String toUsername;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }
    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
    public Integer getShareType() { return shareType; }
    public void setShareType(Integer shareType) { this.shareType = shareType; }
    public Integer getPermission() { return permission; }
    public void setPermission(Integer permission) { this.permission = permission; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getQuestionTitle() { return questionTitle; }
    public void setQuestionTitle(String questionTitle) { this.questionTitle = questionTitle; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }
    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }
}
