package com.qraudio.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenUtil jwtTokenUtil;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // Tìm token
        final String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        // Tách token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {

            // Cắt bỏ chữ "Bearer " để lấy token
            jwt = authorizationHeader.substring(7);
            try {

                // Lấy username từ Token
                username = jwtTokenUtil.getUsernameFromToken(jwt);
            } catch (Exception e) {

                // Token không hợp lệ hoặc đã hết hạn
                logger.error("Token JWT không hợp lệ: " + e.getMessage());
            }
        }

        // Username có nhưng chưa được xác thực 
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // Kiểm tra Token - true: lưu vào hệ thống, false: bỏ qua
            if (jwtTokenUtil.validateToken(jwt)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, new ArrayList<>());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
    }
}