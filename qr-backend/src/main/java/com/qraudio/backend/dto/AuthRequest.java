package com.qraudio.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {
    @NotBlank
    @Size(max = 60)
    private String username;        // Tên đăng nhập
    @NotBlank
    @Size(max = 72)
    private String password;        // Mật khẩu

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
