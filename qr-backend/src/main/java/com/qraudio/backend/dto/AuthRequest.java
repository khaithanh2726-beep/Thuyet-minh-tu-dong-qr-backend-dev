package com.qraudio.backend.dto;

public class AuthRequest {
    private String username;        // Tên đăng nhập
    private String password;        // Mật khẩu

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}