package com.qraudio.backend.dto;

public class AuthResponse {
    private String token;       // Token

    public AuthResponse(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}