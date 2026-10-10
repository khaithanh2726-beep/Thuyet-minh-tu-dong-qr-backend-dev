package com.qraudio.backend.controller;

import com.qraudio.backend.dto.ApiResponse;
import com.qraudio.backend.dto.AuthRequest;
import com.qraudio.backend.dto.AuthResponse;
import com.qraudio.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ApiResponse.success("Đăng nhập thành công", authService.login(request));
    }
}
