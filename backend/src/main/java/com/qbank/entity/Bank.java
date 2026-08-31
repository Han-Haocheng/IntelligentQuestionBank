package com.qbank.entity;

import java.time.LocalDateTime;

/**
 * 题库(题目分组)
 */
public class Bank {

    private Long id;
    private String name;
    private String description;
    private Long originBankId;  // 拷贝来源题库ID
    private Long userId;
    private LocalDateTime createTime;

    /** 非数据库字段: 题库内题目数量 */
    private Long questionCount;
    /** 非数据库字段: 共享状态 - sharedByMe: 1我共享过; incomingPermission: 收到权限 1只读 2可编辑(null=未收到) */
    private Integer sharedByMe;
    private Integer incomingPermission;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getOriginBankId() { return originBankId; }
    public void setOriginBankId(Long originBankId) { this.originBankId = originBankId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Long getQuestionCount() { return questionCount; }
    public void setQuestionCount(Long questionCount) { this.questionCount = questionCount; }
    public Integer getSharedByMe() { return sharedByMe; }
    public void setSharedByMe(Integer sharedByMe) { this.sharedByMe = sharedByMe; }
    public Integer getIncomingPermission() { return incomingPermission; }
    public void setIncomingPermission(Integer incomingPermission) { this.incomingPermission = incomingPermission; }
}