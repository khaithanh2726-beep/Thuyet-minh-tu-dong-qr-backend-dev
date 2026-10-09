package com.qraudio.backend.security;

import org.junit.jupiter.api.Test;

import com.qraudio.backend.security.JwtTokenUtil;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilTest {
    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil();

    @Test
    // Hàm kiểm tra việc tạo và xác thực Token thật
    void generateAndValidateToken_Success() {
        String username = "admin";
        String token = jwtTokenUtil.generateToken(username);
        assertNotNull(token, "Token không được null");
        assertTrue(jwtTokenUtil.validateToken(token), "Token phải hợp lệ");
        assertEquals(username, jwtTokenUtil.getUsernameFromToken(token), "Username giải mã phải khớp");
    }

    @Test
    // Hàm kiểm tra việc xác thực Token giả
    void validateToken_InvalidToken_ReturnsFalse() {
        String invalidToken = "chuoi.token.gia.khong.hop.le";
        assertFalse(jwtTokenUtil.validateToken(invalidToken), "Token giả phải bị từ chối");
    }
}