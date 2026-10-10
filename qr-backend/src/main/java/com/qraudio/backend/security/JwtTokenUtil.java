package com.qraudio.backend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenUtil {

    private final Key key;
    private final long expirationMs;

    public JwtTokenUtil(
            @Value("${security.jwt.secret}") String secretBase64,
            @Value("${security.jwt.expiration-ms}") long expirationMs) {

        if (expirationMs <= 0) {
            throw new IllegalArgumentException(
                    "Thời hạn JWT phải lớn hơn 0");
        }

        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    // Hàm tạo Token
    public String generateToken(String username, Long userId, long tokenVersion) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("ver", tokenVersion)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    // Hàm lấy username từ Token
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    // Hàm kiểm tra Token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
