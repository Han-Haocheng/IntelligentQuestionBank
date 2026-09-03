package com.qbank.entity;

import java.time.LocalDateTime;

/**
 * 前端样式主题
 */
public class Theme {

    private Long id;
    private String name;
    private String themeKey;   // 主题标识(唯一)
    private String config;     // 样式配置 JSON
    private Integer enabled;   // 1启用 0停用
    private Integer isDefault; // 1全局默认 0否
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getThemeKey() { return themeKey; }
    public void setThemeKey(String themeKey) { this.themeKey = themeKey; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}