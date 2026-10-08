package com.qraudio.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenUtil {
    private final String SECRET_KEY = "day-la-khoa-bi-mat-sieu-cap-cua-qr-audio-backend-dev-can-dai-hon";
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    private final long EXPIRATION_TIME = 86400000;          // Hiệu lực Token

    // Hàm tạo Token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date()) // Thời điểm tạo
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Thời điểm hết hạn
                .signWith(key, SignatureAlgorithm.HS256) // Ký bằng khóa bí mật
                .compact();
    }

    // Hàm lấy username từ Token
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Hàm kiểm tra Token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}