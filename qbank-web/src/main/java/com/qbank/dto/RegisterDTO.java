package com.qbank.dto;

/**
 * 注册/管理员新增用户入参
 */
public class RegisterDTO {
    private String username;
    private String password;
    private String nickname;
    private Integer role;    // 管理员新增用户时可指定, 默认1

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
}
