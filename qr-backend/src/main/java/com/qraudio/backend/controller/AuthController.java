package com.qraudio.backend.controller;

import com.qraudio.backend.security.JwtTokenUtil;
import com.qraudio.backend.dto.ApiResponse;
import com.qraudio.backend.dto.AuthRequest;
import com.qraudio.backend.dto.AuthResponse;
import com.qraudio.backend.entity.User;
import com.qraudio.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody AuthRequest request) {
        // Tìm username
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        // Xác thực username và password
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Sai tài khoản hoặc mật khẩu");
        }

        // Tạo và xuất Token
        String token = jwtTokenUtil.generateToken(user.getUsername());
        return ApiResponse.success("Đăng nhập thành công", new AuthResponse(token));
    }
}