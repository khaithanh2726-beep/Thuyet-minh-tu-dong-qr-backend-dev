package com.qraudio.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qraudio.backend.dto.AuthRequest;
import com.qraudio.backend.entity.User;
import com.qraudio.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Optional;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void login_WithValidCredentials_ReturnsToken() throws Exception {
        // Giả lập dữ liệu trong DB
        User mockUser = new User();
        mockUser.setUsername("testadmin");
        mockUser.setPasswordHash(passwordEncoder.encode("123456"));
        when(userRepository.findByUsername("testadmin")).thenReturn(Optional.of(mockUser));
        AuthRequest request = new AuthRequest();
        request.setUsername("testadmin");
        request.setPassword("123456");

        // Gọi API và kiểm tra
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists());
    }
}