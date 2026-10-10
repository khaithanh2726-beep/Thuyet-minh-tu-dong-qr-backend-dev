package com.qraudio.backend.service;

import com.qraudio.backend.dto.AuthRequest;
import com.qraudio.backend.dto.AuthResponse;
import com.qraudio.backend.entity.User;
import com.qraudio.backend.exception.ApiException;
import com.qraudio.backend.repository.UserRepository;
import com.qraudio.backend.security.JwtTokenUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user == null || !user.isActive()
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Thông tin đăng nhập không hợp lệ hoặc tài khoản đã bị vô hiệu hóa");
        }
        return new AuthResponse(jwtTokenUtil.generateToken(
                user.getUsername(), user.getId(), user.getTokenVersion()));
    }
}
