package com.qraudio.backend.security;

import org.junit.jupiter.api.Test;
import com.qraudio.backend.security.JwtTokenUtil;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilTest {
    // Khóa giả chỉ phục vụ unit test.
    private static final String TEST_SECRET =
        Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef"
                        .getBytes(StandardCharsets.UTF_8));
    private final JwtTokenUtil jwtTokenUtil =
        new JwtTokenUtil(TEST_SECRET, 86400000L);

    @Test
    // Hàm kiểm tra việc tạo và xác thực Token thật
    void generateAndValidateToken_Success() {
        String username = "admin";
        String token = jwtTokenUtil.generateToken(username, 1L, 0L);
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

    @Test
    void validateToken_WithDifferentKey_ReturnsFalse() {
        String anotherSecret = Base64.getEncoder().encodeToString(
            "abcdef0123456789abcdef0123456789"
                    .getBytes(StandardCharsets.UTF_8));

        JwtTokenUtil anotherJwtTokenUtil =
            new JwtTokenUtil(anotherSecret, 86400000L);

        String token = anotherJwtTokenUtil.generateToken("admin", 1L, 0L);

    assertFalse(jwtTokenUtil.validateToken(token));
}
}
