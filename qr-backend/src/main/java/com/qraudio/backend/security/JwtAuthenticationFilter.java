package com.qraudio.backend.security;

import com.qraudio.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserRepository userRepository) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = null;
            Long userId = null;
            Long version = null;
            try {
                claims = jwtTokenUtil.getClaimsFromToken(header.substring(7));
                userId = claims.get("uid", Long.class);
                version = claims.get("ver", Long.class);
            } catch (JwtException | IllegalArgumentException exception) {
                // Never log credentials or parser exception details.
                SecurityContextHolder.clearContext();
            }
            if (claims != null && userId != null && version != null) {
                String subject = claims.getSubject();
                long expectedVersion = version;
                userRepository.findById(userId)
                        .filter(user -> user.isActive()
                                && Objects.equals(subject, user.getUsername())
                                && expectedVersion == user.getTokenVersion())
                        .ifPresent(user -> {
                            List<SimpleGrantedAuthority> authorities = user.getRole() == null
                                    ? List.of()
                                    : List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                            var authentication = new UsernamePasswordAuthenticationToken(
                                    user.getUsername(), null, authorities);
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });
            }
        }
        chain.doFilter(request, response);
    }
}
